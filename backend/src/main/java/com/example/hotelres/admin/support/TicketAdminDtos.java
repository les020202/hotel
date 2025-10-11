package com.example.hotelres.admin.support;

import com.example.hotelres.support.SupportMessage;
import com.example.hotelres.support.SupportTicket;
import com.example.hotelres.user.User;

import java.time.LocalDateTime;
import java.util.List;

public final class TicketAdminDtos {

    private TicketAdminDtos() {}

    /** 리스트용 행 DTO */
    public record TicketRowDto(
            Long id,
            Long userId,
            String userName,
            String subject,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static TicketRowDto from(SupportTicket t) {
            User u = t.getUser();
            return new TicketRowDto(
                    t.getId(),
                    u != null ? u.getId() : null,
                    u != null ? u.getName() : null,
                    t.getSubject(),
                    t.getStatus().name(),
                    t.getCreatedAt(),
                    t.getUpdatedAt()
            );
        }
    }

    /** 메시지 DTO */
    public record MessageDto(
            Long id,
            Long senderId,
            String senderName,
            String content,
            LocalDateTime createdAt
    ) {
        public static MessageDto from(SupportMessage m) {
            User s = m.getSender();
            return new MessageDto(
                    m.getId(),
                    s != null ? s.getId() : null,
                    s != null ? s.getName() : null,
                    m.getContent(),
                    m.getCreatedAt()
            );
        }
    }

    /** 상세(메시지 포함) DTO */
    public record TicketDetailDto(
            Long id,
            Long userId,
            String userName,
            String subject,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<MessageDto> messages
    ) {
        public static TicketDetailDto from(SupportTicket t, List<SupportMessage> msgs) {
            User u = t.getUser();
            return new TicketDetailDto(
                    t.getId(),
                    u != null ? u.getId() : null,
                    u != null ? u.getName() : null,
                    t.getSubject(),
                    t.getStatus().name(),
                    t.getCreatedAt(),
                    t.getUpdatedAt(),
                    msgs.stream().map(MessageDto::from).toList()
            );
        }
    }
}
