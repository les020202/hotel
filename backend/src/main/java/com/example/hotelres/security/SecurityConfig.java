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
                // 세션 필요 시에만 생성 (OAuth2 플로우 위해)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // ------------ 공개 리소스 ------------
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/", "/index.html", "/favicon.ico", "/assets/**",
                                "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health",
                                "/error", "/error/**",
                                "/files/**",     // 기존 정적 매핑
                                "/uploads/**"    // ★ 추가: 업로드 정적 매핑
                        ).permitAll()

                        // ------------ 인증/로그인 관련 ------------
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/api/auth/**").permitAll()



                        // ── 결제: 브리지(리디렉션 처리용)가 있다면 허용, 실제 승인은 로그인 필요
                        .requestMatchers(HttpMethod.GET,  "/api/payments/success-bridge").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/confirm").authenticated()   // ✅ 비회원 결제 차단

                        // ------------ 공개 호텔 검색/조회 ------------
                        .requestMatchers(HttpMethod.GET, "/api/hotels/**", "/api/search/**").permitAll()
                        .requestMatchers("/api/amenities/**").permitAll()
                        .requestMatchers("/reservation/**").permitAll()
                        .requestMatchers("/api/time").permitAll()




                        // ------------ 리뷰 공개 조회(비로그인 OK) ------------
                        .requestMatchers(HttpMethod.GET, "/api/hotels/*/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/hotels/*/rating").permitAll()

                        // ------------ 리뷰 작성/업로드/신고(로그인 필요) ------------
                        .requestMatchers(HttpMethod.POST, "/api/reviews").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reviews/*/photo").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reviews/*/report").authenticated()

                        // ── 예약 hold: 생성/조회/삭제 모두 로그인 필요
                        .requestMatchers(HttpMethod.POST,   "/api/reservations/holds/**").authenticated() // ✅
                        .requestMatchers(HttpMethod.GET,    "/api/reservations/holds/**").authenticated() // ✅
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/holds/**").authenticated() // ✅

                        // ------------ 역할별 보호 구간 ------------
                        // ── 오너/관리자 백오피스
                        .requestMatchers("/api/owner/**").hasRole("OWNER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/reviews/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/hotel-applications/**", "/api/admin/**").hasRole("ADMIN")
                        // (운영 배치용 엔드포인트는 역할 제한)

                        .requestMatchers("/api/reservations/holds/release-expired").hasRole("ADMIN")

                        // ------------ 그 외 ------------
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

    /**
     * 제공자별 계정선택/재로그인 강제 파라미터 삽입
     */
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
