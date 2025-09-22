package com.example.hotelres.support;  
// 고객지원(티켓/메시지) 관련 패키지

import com.example.hotelres.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "support_messages")
// JPA 엔티티, 매핑 테이블 이름: support_messages
@Getter @Setter @NoArgsConstructor
// Lombok: getter/setter, 기본 생성자 자동 생성
public class SupportMessage {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 기본 키, AUTO_INCREMENT

    @ManyToOne(optional = false) 
    @JoinColumn(name = "ticket_id")
    private SupportTicket ticket;
    // 다대일 관계: 여러 메시지가 하나의 티켓에 속함
    // FK 컬럼: ticket_id

    @ManyToOne(optional = false) 
    @JoinColumn(name = "sender_id")
    private User sender;
    // 다대일 관계: 여러 메시지가 특정 사용자(User)에 의해 작성됨
    // FK 컬럼: sender_id

    @Lob 
    @Column(nullable = false)
    private String content;
    // 메시지 본문 (긴 텍스트 가능, CLOB)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    // 메시지 작성 시각 (기본값: 현재 시간)
}
