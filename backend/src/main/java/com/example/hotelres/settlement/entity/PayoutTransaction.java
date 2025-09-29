// src/main/java/com/example/hotelres/settlement/entity/PayoutTransaction.java
package com.example.hotelres.settlement.entity;

import com.example.hotelres.settlement.entity.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payout_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayoutTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="statement_id", nullable=false)
    private Long statementId;   // FK만 들고가도 충분

    @Column(name="requested_amount", nullable=false)
    private Long requestedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private PayoutStatus status;  // 

    @Column(name="requested_at", nullable=false)
    private LocalDateTime requestedAt;

    @Column(name="completed_at")
    private LocalDateTime completedAt;

    @Column(name="fail_code")
    private String failCode;

    @Column(name="fail_message", length=500)
    private String failMessage;
}
