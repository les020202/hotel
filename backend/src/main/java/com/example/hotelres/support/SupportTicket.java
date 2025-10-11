package com.example.hotelres.support;  
// 고객지원(티켓/메시지) 관련 패키지

import com.example.hotelres.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity @Table(name = "support_tickets") // JPA 엔티티이며 support_tickets 테이블과 매핑
@Getter @Setter @NoArgsConstructor        // Lombok: getter/setter, 기본 생성자 자동 생성
public class SupportTicket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본 키(자동 증가)

    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    private User user; // 티켓을 생성한 사용자(FK: user_id)

    @Column(nullable = false, length = 150)
    private String subject; // 문의 제목(최대 150자)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.UPLOADED; // 티켓 상태(문자열 저장). 기본값: UPLOADED
    public enum Status { UPLOADED, ANSWERED } // 상태 값: 접수(UPLOADED), 답변완료(ANSWERED)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성 시각

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now(); // 수정 시각

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupportMessage> messages = new ArrayList<>(); // 티켓에 속한 메시지 목록(양방향, 고아 제거)

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now(); // 영속화 직전 생성 시각 보정
        if (updatedAt == null) updatedAt = createdAt;           // 최초에는 수정 시각 = 생성 시각
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); } // 업데이트 직전 수정 시각 갱신
}
