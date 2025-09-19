package com.example.hotelres.support;  
// 고객지원(티켓/메시지) 관련 패키지

import com.example.hotelres.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "support_messages")
@Getter @Setter @NoArgsConstructor
public class SupportMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "ticket_id")
    private SupportTicket ticket;

    @ManyToOne(optional = false) @JoinColumn(name = "sender_id")
    private User sender;

    @Lob @Column(nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}