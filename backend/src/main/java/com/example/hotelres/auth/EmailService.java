package com.example.hotelres.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // 보내는 주소(반드시 spring.mail.username과 동일)
    @Value("${spring.mail.username}")
    private String from;

    private static final SecureRandom RND = new SecureRandom();

    /** 6자리 숫자 코드 생성 (000000~999999가 아닌 100000~999999) */
    public String generate6Digit() {
        return String.valueOf(100000 + RND.nextInt(900000));
    }

    /** 이미 생성한 code를 받아 전송 (컨트롤러에서 저장/검증에 쓰기 좋음) */
    public void sendVerificationCode(String toEmail, String code) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        h.setFrom(from);                    // ✅ 꼭 설정 (보내는 주소 정책)
        h.setTo(toEmail);
        h.setSubject("[HOTELRES] 이메일 인증 코드");
        h.setText(
                "<div style='font-family:system-ui,Segoe UI,Roboto,Apple SD Gothic Neo,sans-serif'>"
                        + "<p>요청하신 이메일 인증 코드입니다.</p>"
                        + "<p>인증 코드: <b style='font-size:18px'>" + code + "</b></p>"
                        + "<p style='color:#666'>5분 내에 입력해주세요.</p>"
                        + "</div>", true);
        mailSender.send(msg);
    }

    /** 편의 메서드: 코드 생성 + 전송 후 코드 반환 (원하면 사용) */
    public String sendAndReturnCode(String toEmail) throws MessagingException {
        String code = generate6Digit();
        sendVerificationCode(toEmail, code);
        return code;
    }

}
