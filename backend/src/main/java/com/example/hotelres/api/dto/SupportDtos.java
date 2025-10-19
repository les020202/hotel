package com.example.hotelres.api.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

import com.example.hotelres.support.SupportTicket;

public class SupportDtos {
    // ── Notices
    public record NoticeBrief(
        Long id,
        String title,
        boolean pinned,
        LocalDateTime createdAt
    ) {}
    public record NoticeDetail(
        Long id,
        String title,
        String content,
        boolean pinned,
        LocalDateTime createdAt
    ) {}
    public record NoticeNavRes(Long prevId, Long nextId) {}

    // ── FAQs
    public record FaqItem(
        Long id,
        String category,
        String question,
        String answer
    ) {}

    // ── Tickets & Messages
    public record TicketNewReq(
        @Size(max = 150)  String subject,       // 제목은 컨트롤러에서 null 체크
        @Size(max = 8000) String firstMessage   // 최초 메시지는 optional(기존 로직 유지)
    ) {}

    public record TicketBrief(Long id, String subject, String status, LocalDateTime createdAt) {
        public static TicketBrief of(SupportTicket t) {
            return new TicketBrief(t.getId(), t.getSubject(), t.getStatus().name(), t.getCreatedAt());
        }
    }
    public record TicketsRes(List<TicketBrief> items) {}

    public record MessageReq(
        @Size(max = 8000) String content  // 본문 길이 제한만 — 필수 여부는 컨트롤러에서 검사
    ) {}

    public record MessageRes(
        Long id,
        Long senderId,
        String senderRole,
        String content,
        LocalDateTime createdAt
    ) {}
}
