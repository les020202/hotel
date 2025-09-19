package com.example.hotelres.support;  
// 고객지원(티켓/메시지) 관련 패키지

import com.example.hotelres.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity 
@Table(name = "support_tickets")
// JPA 엔티티, 매핑 테이블 이름: support_tickets
@Getter @Setter @NoArgsConstructor
// Lombok: getter/setter, 기본 생성자 자동 생성
public class SupportTicket {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 기본 키, AUTO_INCREMENT

    @ManyToOne(optional = false) 
    @JoinColumn(name = "user_id")
    private User user;
    // 다대일 관계: 여러 티켓이 하나의 사용자(User)에 속함
    // FK 컬럼: user_id

    @Column(nullable = false, length = 150)
    private String subject;
    // 티켓 제목 (최대 150자)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.UPLOADED;
    // 상태값 (문자열로 저장)
    // 기본값: UPLOADED

    public enum Status { UPLOADED, ANSWERED }
    // 티켓 상태: 등록됨 / 답변 완료됨

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    // 생성일시 (기본: 현재 시간)

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    // 수정일시 (기본: 현재 시간)

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupportMessage> messages = new ArrayList<>();
    // 티켓 ↔ 메시지 (1:N)
    // mappedBy="ticket" → SupportMessage.ticket 이 FK 담당
    // cascade=ALL → 티켓 저장/삭제 시 메시지도 같이 처리
    // orphanRemoval=true → messages 리스트에서 제거하면 DB에서도 삭제

    @PreUpdate
    void onUpdate() { 
        this.updatedAt = LocalDateTime.now(); 
    }
    // 엔티티 업데이트 직전에 updatedAt 필드 자동 갱신
}
