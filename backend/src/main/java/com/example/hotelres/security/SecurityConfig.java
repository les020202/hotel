// src/main/java/com/example/hotelres/security/SecurityConfig.java
package com.example.hotelres.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl uds;

    // OAuth2
    private final OAuth2UserServiceImpl oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOriginsProp;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        var p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            // 🔐 보안 헤더 (기존 유지 + 보강)
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'self'"))
                // X-Frame-Options: SAMEORIGIN (H2 콘솔 등 필요 시 sameOrigin 유지)
                .frameOptions(frame -> frame.sameOrigin())
                // X-Content-Type-Options: nosniff
                .contentTypeOptions(withDefaults())
                // Referrer-Policy
                .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                // HSTS (HTTPS 환경에서만 효과; 로컬 HTTP에서는 브라우저가 무시)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .preload(true)
                    .maxAgeInSeconds(31536000))
            )

            .authorizeHttpRequests(auth -> auth
                // 공개 리소스
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/", "/index.html", "/favicon.ico", "/assets/**",
                    "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health",
                    "/error", "/error/**",
                    "/files/**",
                    "/uploads/**"
                ).permitAll()

                // 인증/로그인 관련
                .requestMatchers("/oauth2/**", "/login/oauth2/**", "/api/auth/**").permitAll()

                // 결제
                .requestMatchers(HttpMethod.GET,  "/api/payments/success-bridge").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/confirm").authenticated()

                // 공개 호텔 검색/조회
                .requestMatchers(HttpMethod.GET, "/api/hotels/**", "/api/search/**").permitAll()
                .requestMatchers("/api/amenities/**").permitAll()
                .requestMatchers("/reservation/**").permitAll()
                .requestMatchers("/api/time").permitAll()

                // (기존 유지)
                .requestMatchers("/api/hotelapp/**").authenticated()

                // ✅ 리뷰 공개 조회(비로그인 허용)
                .requestMatchers(HttpMethod.GET, "/api/hotels/*/reviews").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/hotels/*/reviews/rating").permitAll()

                // 리뷰 작성/업로드/신고
                .requestMatchers(HttpMethod.POST, "/api/reviews").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/reviews/*/photo").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/reviews/*/report").authenticated()

                // 예약 hold
                .requestMatchers(HttpMethod.POST,   "/api/reservations/holds/**").authenticated()
                .requestMatchers(HttpMethod.GET,    "/api/reservations/holds/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/reservations/holds/**").authenticated()

                // 역할별
                .requestMatchers("/api/owner/**").hasRole("OWNER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/reviews/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/hotel-applications/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/reservations/holds/release-expired").hasRole("ADMIN")
                
                //에러 메시지 테스트용
                .requestMatchers("/api/test/**").permitAll()

                // 그 외
                .requestMatchers("/api/coupons/**").authenticated()
                .anyRequest().authenticated()
                
            )

            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // ❗ 인증 실패/권한 거부 → 표준 JSON + X-Error-Id 헤더로 응답
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                    writeJsonError(res, HttpServletResponse.SC_UNAUTHORIZED,
                        "UNAUTHORIZED", "로그인이 필요합니다."))
                .accessDeniedHandler((req, res, ex) ->
                    writeJsonError(res, HttpServletResponse.SC_FORBIDDEN,
                        "FORBIDDEN", "접근 권한이 없습니다."))
            )

            .oauth2Login(oauth -> oauth
                .authorizationEndpoint(ep -> ep
                    .baseUri("/oauth2/authorization")
                    .authorizationRequestRepository(
                        new org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository())
                    .authorizationRequestResolver(customAuthorizationRequestResolver(clientRegistrationRepository))
                )
                .redirectionEndpoint(re -> re.baseUri("/login/oauth2/code/*"))
                .userInfoEndpoint(ui -> ui.userService(oAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler((req, res, ex) -> {
                    String origin = req.getHeader("Origin");
                    if (origin == null || origin.isBlank()) origin = "http://localhost:5173";
                    String target = origin + "/login?social_error=" +
                            java.net.URLEncoder.encode(
                                ex.getMessage() != null ? ex.getMessage() : "OAuth2_failed",
                                StandardCharsets.UTF_8);
                    res.setStatus(302);
                    res.sendRedirect(target);
                })
            )

            .logout(l -> l
                .logoutUrl("/logout")
                .deleteCookies("JSESSIONID", "refreshToken")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
            )

            .authenticationProvider(authProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(
            ClientRegistrationRepository repo) {

        DefaultOAuth2AuthorizationRequestResolver delegate =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return customize(delegate.resolve(request));
            }
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                return customize(delegate.resolve(request, clientRegistrationId));
            }
            private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest req) {
                if (req == null) return null;

                String regId = (String) req.getAttributes().get(OAuth2ParameterNames.REGISTRATION_ID);
                Map<String, Object> extra = new HashMap<>(req.getAdditionalParameters());

                if ("google".equals(regId)) extra.put("prompt", "select_account");
                if ("kakao".equals(regId))  extra.put("prompt", "login");
                if ("naver".equals(regId))  extra.put("auth_type", "reprompt");

                return OAuth2AuthorizationRequest.from(req)
                        .additionalParameters(extra)
                        .build();
            }
        };
    }

    // CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 1) env에서 콤마 분리
        List<String> fromProp = Arrays.stream(allowedOriginsProp.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList());
        // 2) fallback 하드코딩 목록
        List<String> fallback = List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://172.16.15.53:5173"
        );
        List<String> origins = fromProp.isEmpty() ? fallback : fromProp;

        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /* ------------------------ helpers ------------------------ */

    private void writeJsonError(HttpServletResponse res, int status, String code, String message) throws IOException {
        String errorId = UUID.randomUUID().toString();
        res.setStatus(status);
        res.setHeader("X-Error-Id", errorId);
        res.setHeader("Cache-Control", "no-store");

        String json = """
            {"status":%d,"code":"%s","message":"%s","errorId":"%s"}
            """.formatted(status, escape(code), escape(message), errorId);

        res.setContentType("application/json;charset=UTF-8");
        res.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
