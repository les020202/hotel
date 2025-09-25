// src/main/java/com/example/hotelres/auth/EmailService.java
package com.example.hotelres.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
// import org.springframework.scheduling.annotation.Async; // 비동기 원하면 해제

import java.security.SecureRandom;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    private static final SecureRandom RND = new SecureRandom();

    public String generate6Digit() {
        return String.valueOf(100000 + RND.nextInt(900000));
    }

    public void sendVerificationCode(String toEmail, String code) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        h.setFrom(from);
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

    public String sendAndReturnCode(String toEmail) throws MessagingException {
        String code = generate6Digit();
        sendVerificationCode(toEmail, code);
        return code;
    }

    // ================== 예약확인 메일 ==================

    public record BookingMailPayload(
        String toEmail,
        String customerName,   // 없으면 "고객님"
        Long   bookingId,      // 예약번호
        String hotelName,      // 없으면 빈 문자열 허용
        String roomTypeName,   // 없으면 빈 문자열 허용
        LocalDate checkIn,
        LocalDate checkOut,
        int     guests,        // ← Integer -> int 로 변경
        String  amountHuman    // "120,000원"
    ) {}

    /** 예약확인 메일 전송 (템플릿 엔진 없이 순수 HTML) */
    // @Async // 비동기 원하면 주석 해제
    public void sendBookingConfirmation(BookingMailPayload p) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(from);
            h.setTo(p.toEmail());
            h.setSubject("[HOTELRES] 예약이 확정되었습니다 (#" + p.bookingId() + ")");

            int nights = 0;
            if (p.checkIn() != null && p.checkOut() != null) {
                nights = Math.max(0, (int) java.time.temporal.ChronoUnit.DAYS.between(p.checkIn(), p.checkOut()));
            }
            String name = (p.customerName() == null || p.customerName().isBlank()) ? "고객님" : p.customerName();

            StringBuilder html = new StringBuilder();
            html.append("<div style='font-family:system-ui,Segoe UI,Roboto,Apple SD Gothic Neo,sans-serif;color:#111'>");
            html.append("<div style='max-width:640px;margin:24px auto;padding:24px;border:1px solid #e5e7eb;border-radius:12px'>");
            html.append("<h2 style='margin:0 0 12px 0;font-size:20px;font-weight:700'>예약이 확정되었어요 🎉</h2>");
            html.append("<p><b>").append(escape(name)).append("</b> 님, 아래 내용으로 예약이 완료되었습니다.</p>");

            html.append("<div style='display:grid;grid-template-columns:140px 1fr;gap:8px;margin-top:12px'>");
            html.append("<div style='color:#6b7280'>예약번호</div><div>").append(p.bookingId()).append("</div>");
            html.append("<div style='color:#6b7280'>호텔</div><div>").append(escape(nvl(p.hotelName()))).append("</div>");
            html.append("<div style='color:#6b7280'>객실타입</div><div>").append(escape(nvl(p.roomTypeName()))).append("</div>");
            html.append("<div style='color:#6b7280'>체크인</div><div>")
                .append(p.checkIn() == null ? "" : p.checkIn()).append("</div>");
            html.append("<div style='color:#6b7280'>체크아웃</div><div>")
                .append(p.checkOut() == null ? "" : p.checkOut());
            if (nights > 0) {
                html.append(" <span style='color:#6b7280'>(").append(nights).append("박)</span>");
            }
            html.append("</div>");
            html.append("<div style='color:#6b7280'>인원</div><div>")
                .append(p.guests()).append("명</div>");
            html.append("<div style='color:#6b7280'>결제금액</div><div>")
                .append(escape(nvl(p.amountHuman()))).append("</div>");
            html.append("</div>"); // grid

            html.append("<p style='color:#6b7280;margin-top:16px'>예약내역은 [마이페이지 &gt; 예약내역]에서도 확인할 수 있어요.</p>");
            html.append("</div></div>");

            h.setText(html.toString(), true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            throw new RuntimeException("예약확인 메일 전송 실패", e);
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
    private static String nvl(String s) { return s == null ? "" : s; }
}
