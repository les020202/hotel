package com.example.hotelres.security;

import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2UserServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository users;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User user = delegate.loadUser(req);

        String provider = req.getClientRegistration().getRegistrationId(); // google/kakao/naver
        Map<String, Object> attrs = user.getAttributes();

        String externalId;
        String email = null;
        String name  = null;

        switch (provider) {
            case "google" -> {
                externalId = (String) attrs.get("sub");               // openid 필요
                email = (String) attrs.get("email");
                name  = (String) attrs.get("name");
            }
            case "kakao" -> {
                externalId = String.valueOf(attrs.get("id"));
                Map<String, Object> account = (Map<String, Object>) attrs.get("kakao_account");
                if (account != null) {
                    email = (String) account.get("email");            // 검수 전이면 null 가능
                    Map<String, Object> profile = (Map<String, Object>) account.get("profile");
                    if (profile != null) name = (String) profile.get("nickname");
                }
            }
            case "naver" -> {
                Map<String, Object> resp = (Map<String, Object>) attrs.get("response");
                externalId = (String) resp.get("id");
                email = (String) resp.get("email");
                name  = (String) resp.get("name");
            }
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
        }

        String loginId = provider + "_" + externalId;
        String resolvedName  = (name  != null && !name.isBlank())  ? name  : (provider.toUpperCase() + " 사용자");
        String resolvedEmail = (email != null && !email.isBlank()) ? email : (loginId + "@example.local");

        // 1) 로그인아이디로 먼저 조회
        var uOpt = users.findByLoginId(loginId);

        // 2) 없으면 이메일로 병합(기존 로컬계정에 소셜 연결)
        if (uOpt.isEmpty()) {
            var byEmail = users.findByEmail(resolvedEmail);
            if (byEmail.isPresent()) {
                User u = byEmail.get();
                // 이미 같은 provider loginId로 묶여있지 않다면 묶기
                u.setLoginId(loginId);
                users.saveAndFlush(u);
                uOpt = java.util.Optional.of(u);
            }
        }

        // 3) 그래도 없으면 새로 생성 (NOT NULL 필드만 채우고, 생일/성별은 null로)
        User u = uOpt.orElseGet(() -> {
            User nu = new User();
            nu.setLoginId(loginId);
            nu.setPasswordHash("{noop}SOCIAL");   // 소셜은 비밀번호 미사용
            nu.setName(resolvedName);
            nu.setEmail(resolvedEmail);

            // ✅ 임의 값 제거: 제공 안 되면 그대로 null 저장
            nu.setGender(null);
            nu.setBirthDate(null);

            nu.setStatus(User.Status.ACTIVE);
            nu.setRole(User.Role.ROLE_USER);
            return users.saveAndFlush(nu);        // 즉시 플러시
        });

        // 스프링 시큐리티 Principal 생성
        return new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority(u.getRole().name())),
            attrs,
            req.getClientRegistration()
               .getProviderDetails()
               .getUserInfoEndpoint()
               .getUserNameAttributeName()
        );
    }
}
