package com.example.hotelres.payment;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity @Table(name = "payments")
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String paymentKey;   // PG의 거래 키

    @Column(nullable=false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private PaymentStatus status;

    @Column(nullable=false)
    private int amount;

    private String methodLabel;  // "간편결제 · 카카오페이" 등
    private String receiptUrl;
    private OffsetDateTime approvedAt;

    @Lob
    private String rawPayload;   // 원본 JSON 문자열(감사/분석용)
}
