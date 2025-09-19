package com.example.hotelres.security;

import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final JwtUtil jwt;
    private final UserRepository users;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth)
            throws IOException {

        OAuth2User principal = (OAuth2User) auth.getPrincipal();
        String provider = ((OAuth2AuthenticationToken) auth).getAuthorizedClientRegistrationId(); // google/kakao/naver

        // ---- provider별 attribute 안전 추출 ----
        String externalId;
        String email = null;
        String name  = null;

        switch (provider) {
            case "google" -> {
                externalId = Objects.toString(principal.getAttribute("sub"), null);
                email      = Objects.toString(principal.getAttribute("email"), null);
                name       = Objects.toString(principal.getAttribute("name"), null);
            }
            case "kakao" -> {
                externalId = Objects.toString(principal.getAttribute("id"), null);
                Map<String, Object> account = principal.getAttribute("kakao_account");
                if (account != null) {
                    email = Objects.toString(account.get("email"), null);  // 검수 전이면 null 가능
                    Object profileObj = account.get("profile");
                    if (profileObj instanceof Map<?, ?> profile) {
                        name = Objects.toString(profile.get("nickname"), null);
                    }
                }
            }
            case "naver" -> {
                Object responseObj = principal.getAttribute("response");
                if (responseObj instanceof Map<?, ?> resp) {
                    externalId = Objects.toString(resp.get("id"), null);
                    email      = Objects.toString(resp.get("email"), null);
                    name       = Objects.toString(resp.get("name"), null);
                } else {
                    throw new IllegalStateException("Unexpected Naver response type: " +
                            (responseObj == null ? "null" : responseObj.getClass()));
                }
            }
            default -> throw new IllegalStateException("Unsupported provider: " + provider);
        }

        if (externalId == null) {
            throw new IllegalStateException("Missing external id from " + provider + " user info");
        }

        String loginId = provider + "_" + externalId;
        String resolvedName  = (name  != null && !name.isBlank())  ? name  : (provider.toUpperCase() + " 사용자");
        String resolvedEmail = (email != null && !email.isBlank()) ? email : (loginId + "@example.local");

        // 1) loginId로 조회
        Optional<User> userOpt = users.findByLoginId(loginId);

        // 2) 없으면 이메일 병합(기존 계정에 소셜 연결)
        if (userOpt.isEmpty()) {
            var byEmail = users.findByEmail(resolvedEmail);
            if (byEmail.isPresent()) {
                var u = byEmail.get();
                u.setLoginId(loginId);
                users.saveAndFlush(u);
                userOpt = Optional.of(u);
            }
        }

        // 3) 그래도 없으면 즉시 생성 (NOT NULL 컬럼 기본값 세팅)
        User u = userOpt.orElseGet(() -> {
            User nu = new User();
            nu.setLoginId(loginId);
            nu.setPasswordHash("{noop}SOCIAL");     // 소셜은 비밀번호 미사용
            nu.setName(resolvedName);
            nu.setEmail(resolvedEmail);
            nu.setGender(User.Gender.MALE);         // ENUM('MALE','FEMALE') 제약 대응(기본값)
            nu.setBirthDate(LocalDate.of(2000, 1, 1)); // birth_date NOT NULL 기본값
            nu.setStatus(User.Status.ACTIVE);
            nu.setRole(User.Role.ROLE_USER);
            return users.saveAndFlush(nu);          // 즉시 반영
        });

        // JWT 발급 + refresh 쿠키
        String access  = jwt.generateAccess(u.getLoginId(), u.getRole().name());
        String refresh = jwt.generateRefresh(u.getLoginId());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(false)              // 운영 HTTPS에서는 true
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(jwt.getRefreshExpMs() / 1000)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 프론트로 리다이렉트 (encode는 ".name()" 사용)
        String redirect = "http://localhost:5173/main#token=" +
                URLEncoder.encode(access, StandardCharsets.UTF_8.name());
        res.setStatus(HttpServletResponse.SC_FOUND);
        res.setHeader("Location", redirect);
    }
}
