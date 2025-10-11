package com.example.hotelres.support;  
// 고객지원(티켓/메시지) 관련 패키지

import com.example.hotelres.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity @Table(name = "support_messages") // JPA 엔티티: support_messages 테이블과 매핑
@Getter @Setter @NoArgsConstructor          // Lombok: getter/setter, 기본 생성자 생성
public class SupportMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본 키(자동 증가)

    @ManyToOne(optional = false) @JoinColumn(name = "ticket_id")
    private SupportTicket ticket; // 어떤 티켓에 속한 메시지인지(다대일 관계). FK: ticket_id

    @ManyToOne(optional = false) @JoinColumn(name = "sender_id")
    private User sender; // 보낸 사람(사용자). FK: sender_id

    @Lob @Column(nullable = false)
    private String content; // 메시지 본문(길이 제한 없는 텍스트, CLOB)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성 시각(기본값: 현재 시간)

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now(); // 영속화 직전 createdAt이 비어있으면 현재 시간으로 설정
    }
}

