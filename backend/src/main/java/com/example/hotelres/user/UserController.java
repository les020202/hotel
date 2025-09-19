package com.example.hotelres.user;


// 이메일 인증 코드 저장/검증을 위한 컴포넌트

import com.example.hotelres.auth.EmailCodeStore;
import com.example.hotelres.auth.EmailService;
import com.example.hotelres.user.dto.UpdateEmailRequest;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * UserController
 * - 이메일 인증 (발송/검증)
 * - 로그인한 사용자의 비밀번호 변경
 */
@RestController
@RequestMapping("/api")  // 이 컨트롤러의 모든 API는 "/api" 경로 아래에 매핑됨
@RequiredArgsConstructor // final 필드를 자동 생성자 주입
public class UserController {

    // 이메일 발송 서비스 (메일 전송 기능)
    private final EmailService emailService;

    // 이메일 코드 저장소 (코드 생성/저장/검증 기능)
    private final EmailCodeStore emailCodeStore;

    // 사용자 DB 액세스 (User 엔티티 CRUD)
    private final UserRepository userRepository;

    // 비밀번호 암호화/검증에 사용하는 PasswordEncoder (BCrypt 등)
    private final PasswordEncoder passwordEncoder;

    /**
     * [POST] /api/auth/email/send
     * 입력: { "email": "user@example.com" }
     * 동작: 인증 코드 생성 → 저장소에 저장 → 이메일 발송
     */
    @PostMapping("/auth/email/send")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> body) {
        String email = body.get("email"); // JSON body에서 email 추출
        try {
            String code = emailService.generate6Digit(); // 6자리 숫자 코드 생성
            emailCodeStore.save(email, code, 300);       // TTL 300초(5분) 동안 유효
            emailService.sendVerificationCode(email, code); // 메일로 코드 발송
            return ResponseEntity.ok(Map.of("sent", true));
        } catch (Exception e) {
            // 예외 발생 시 500 응답
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getClass().getSimpleName(),
                                 "message", e.getMessage()));
        }
    }

    /**
     * [POST] /api/auth/email/verify
     * 입력: { "email": "user@example.com", "code": "123456" }
     * 동작: 저장소에 있는 코드와 비교하여 유효한지 검증
     * 반환: { "verified": true/false }
     */
    @PostMapping("/auth/email/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        boolean ok = emailCodeStore.check(body.get("email"), body.get("code"));
        return ResponseEntity.ok(Map.of("verified", ok));
    }

    /**
     * [PUT] /api/users/me/password
     * 입력: { "currentPassword": "현재비번", "newPassword": "새비번" }
     * 동작: 
     *   1) 로그인된 사용자의 loginId 가져오기
     *   2) DB에서 해당 User 조회
     *   3) 현재 비밀번호 검증
     *   4) 새 비밀번호를 암호화해서 저장
     */
    @PutMapping("/users/me/password")
    public ResponseEntity<?> changePassword(
            // 현재 로그인한 사용자의 인증 정보 (Spring Security가 자동 주입)
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, String> body) {

        // 로그인 ID 추출
        String loginId = principal.getUsername();

        // DB에서 로그인 ID로 User 조회
        var user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 요청 JSON에서 현재/새 비밀번호 추출
        String currentPassword = body.get("currentPassword");
        String newPassword     = body.get("newPassword");

        // 1) 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            // 입력한 현재 비밀번호가 DB에 저장된 해시와 일치하지 않으면 오류 반환
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "현재 비밀번호가 일치하지 않습니다."));
        }

        // 2) 새 비밀번호 암호화 후 저장
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 성공 응답
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
    }

    
    /**
     * 내 이메일 변경 (이메일 인증코드 필요)
     * - 인증코드는 헤더 X-Verification-Code 또는 body.verificationCode 로 받음(둘 중 하나면 OK)
     * - body 예: { "email": "new@example.com" }
     */
    @PatchMapping("/users/me/email")
    public ResponseEntity<?> updateMyEmail(
            @AuthenticationPrincipal(expression = "username") String loginId,
            @Valid @RequestBody UpdateEmailRequest req
    ) {
        var u = userRepository.findByLoginId(loginId).orElseThrow();

        // 이미 사용 중인 이메일인지 확인
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "EMAIL_IN_USE"));
        }

        // 인증코드 최종 소모(consume) 검증
        boolean ok = emailCodeStore.consume(req.getEmail(), req.getVerificationCode());
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_OR_EXPIRED_CODE"));
        }

        u.setEmail(req.getEmail());
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
