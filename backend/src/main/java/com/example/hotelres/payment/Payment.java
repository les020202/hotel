// path: src/main/java/com/example/hotelres/payment/Payment.java
package com.example.hotelres.payment;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
  indexes = {
    @Index(name="ix_pay_user", columnList = "user_id, created_at DESC"),
    @Index(name="ix_pay_provider_ref", columnList = "provider, provider_ref")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="booking_id") private Long bookingId;          // 확정 후 연결
    @Column(name="user_id", nullable=false) private Long userId;

    @Column(nullable=false, length=30) private String provider; // "toss"
    @Column(nullable=false, length=20) private String method;   // "CARD"|"VBANK"|...

    @Column(nullable=false) private Integer amount;
    @Column(nullable=false, length=3) private String currency = "KRW";

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=12)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name="provider_ref", length=100) private String providerRef; // Toss paymentKey
    @Column(name="client_secret", length=100) private String clientSecret;

    @Column(name="raw_payload", columnDefinition = "json") private String rawPayload;
    @Column(name="approved_at") private LocalDateTime approvedAt;

    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at", insertable=false, updatable=false) private LocalDateTime updatedAt;
}
