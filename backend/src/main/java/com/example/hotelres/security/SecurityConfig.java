package com.example.hotelres.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // ⬅ 추가
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
import java.util.*;
import java.util.stream.Collectors;


@Configuration
@EnableWebSecurity // ⬅ 추가
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl uds;

    // 소셜 로그인
    private final OAuth2UserServiceImpl oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    // ⬅ 아래 config에서 가져온 "프로퍼티 기반 CORS 허용" 기능
    // 쉼표(,)로 여러 개 지정 가능. 기본값은 localhost/127.0.0.1:5173
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
            // ★ 여기만 바꿈: STATELESS → IF_REQUIRED
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트 (퍼미션이 필요 없는 경로들)
                .requestMatchers("/api/owner/**").hasRole("OWNER")
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // 프리플라이트 요청 허용
                .requestMatchers("/confirm", "/pay/**", "/oauth2/**", "/login/oauth2/**", "/api/auth/**")
                .permitAll() // 로그인, 인증 관련
                .requestMatchers("/", "/index.html", "/favicon.ico", "/assets/**", 
                    "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", 
                    "/error", "/error/**", "/files/**", "/api/amenities/**", "/api/search/**")
                .permitAll() // 정적 파일, 헬스 체크 및 API 문서
                // 사용자 요청
                .requestMatchers(HttpMethod.POST, "/api/hotel-applications").hasRole("USER")
                // 관리자 요청
                .requestMatchers("/api/admin/hotel-applications/**", "/api/admin/**").hasRole("ADMIN")
                // 공개된 호텔 검색 API
                .requestMatchers(HttpMethod.GET, "/api/hotels/**", "/api/search/**").permitAll()
                // 기본적으로 모든 다른 요청은 인증 필요
                .requestMatchers("/api/time").permitAll()
                .requestMatchers("/reservation/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/confirm").permitAll()   // ★ 추가
                .requestMatchers(HttpMethod.GET, "/api/payments/success-bridge").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/confirm").permitAll()
                .requestMatchers("/payments/toss/**").permitAll()
                .requestMatchers("/api/reservations/hold/**").permitAll() // 단건 조회는 누구나 가능
                    .requestMatchers(HttpMethod.GET, "/api/reservations/holds/**").permitAll()
                .requestMatchers("/api/reservations/holds/release-expired").hasRole("ADMIN") // 운영 전용은 잠그고
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
                    // ★ 세션 기반 AuthorizationRequest 저장소 명시
                    .authorizationRequestRepository(new org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository())
                    .authorizationRequestResolver(customAuthorizationRequestResolver(clientRegistrationRepository))
                )
                // ★ 콜백 baseUri 명시 (provider 콘솔과 완전히 동일해야 함)
                .redirectionEndpoint(re -> re.baseUri("/login/oauth2/code/*"))
                .userInfoEndpoint(ui -> ui.userService(oAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler((req, res, ex) -> {
                    String origin = req.getHeader("Origin");
                    if (origin == null || origin.isBlank()) origin = "http://localhost:5173"; //http://172.16.15.53:5173
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
        cfg.setAllowedOrigins(List.of(
        	    "http://localhost:5173",
        	    "http://127.0.0.1:5173",
        	    "http://172.16.15.53:5173" // ★ 추가
        	));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
