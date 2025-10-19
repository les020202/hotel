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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import static org.springframework.security.config.Customizer.withDefaults;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

            // 🔐 보안 헤더(버전 호환)
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'self'"))
                // .xssProtection(x -> x.block(true))  // ⛔ Spring Security 6+에서는 제거/미지원일 수 있어 생략
                .frameOptions(frame -> frame.sameOrigin())
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true))
                .contentTypeOptions(withDefaults()) // → X-Content-Type-Options: nosniff
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

                // ✅ 리뷰 공개 조회(비로그인 허용) — 경로 보정
                .requestMatchers(HttpMethod.GET, "/api/hotels/*/reviews").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/hotels/*/reviews/rating").permitAll() // ← 여기!!

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

                // 그 외
                .requestMatchers("/api/coupons/**").authenticated()
                .anyRequest().authenticated()
            )

            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            .exceptionHandling(e -> e.authenticationEntryPoint(
                (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            ))

            .oauth2Login(oauth -> oauth
                .authorizationEndpoint(ep -> ep
                    .baseUri("/oauth2/authorization")
                    .authorizationRequestRepository(new org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository())
                    .authorizationRequestResolver(customAuthorizationRequestResolver(clientRegistrationRepository))
                )
                .redirectionEndpoint(re -> re.baseUri("/login/oauth2/code/*"))
                .userInfoEndpoint(ui -> ui.userService(oAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler((req, res, ex) -> {
                    String origin = req.getHeader("Origin");
                    if (origin == null || origin.isBlank()) origin = "http://localhost:5173";
                    String target = origin + "/login?social_error=" +
                            java.net.URLEncoder.encode(ex.getMessage() != null ? ex.getMessage() : "OAuth2_failed",
                                    java.nio.charset.StandardCharsets.UTF_8);
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
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://172.16.15.53:5173"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
