package com.example.hotelres.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpMethod;


@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl uds;

    // 소셜 로그인
    private final OAuth2UserServiceImpl oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

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
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // ✅ 프리플라이트 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ 공개 엔드포인트
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/search/**", "/api/hotels/**").permitAll()
                        // (선택) 정적/헬스체크
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/assets/**",
                                "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                        .requestMatchers("/error", "/error/**").permitAll()
                        .requestMatchers("/files/**").permitAll()
                        .requestMatchers("/api/amenities/**").permitAll()   // ← 추가
                        .requestMatchers("/api/search/**").permitAll()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )

                // 기본 폼/Basic 비활성
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            // 인증 안됨 → 401
            .exceptionHandling(e -> e.authenticationEntryPoint(
                (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            ))
            .oauth2Login(oauth -> oauth
                .authorizationEndpoint(ep -> ep
                    .baseUri("/oauth2/authorization")
                    // ★ 소셜 재인증 강제 파라미터 주입
                    .authorizationRequestResolver(
                        customAuthorizationRequestResolver(clientRegistrationRepository)
                    )
                )
                .userInfoEndpoint(ui -> ui.userService(oAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler((req, res, ex) -> {
                    String target = "http://localhost:5173/login?social_error=" +
                            URLEncoder.encode(ex.getMessage() != null ? ex.getMessage() : "OAuth2_failed",
                                    StandardCharsets.UTF_8);
                    res.setStatus(302);
                    res.sendRedirect(target);
                })
            )
            .logout(l -> l
                .logoutUrl("/logout")
                .deleteCookies("JSESSIONID", "refreshToken") // 서버 세션/리프레시 쿠키 제거
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
            )
            .authenticationProvider(authProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 소셜 제공자별 ‘다시 로그인/계정 선택’ 강제 파라미터 추가
     *  - google: prompt=select_account
     *  - kakao : prompt=login
     *  - naver : auth_type=reprompt
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

    // 프론트(5173) 허용 CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:5173", "http://172.16.15.53:5173",  "http://127.0.0.1:5173"));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}