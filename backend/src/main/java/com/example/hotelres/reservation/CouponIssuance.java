// path: backend/src/main/java/com/example/hotelres/reservation/CouponIssuance.java
package com.example.hotelres.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.example.hotelres.coupon.Coupon;

@Entity
@Table(name = "coupon_issuance")
@Getter @Setter
public class CouponIssuance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → coupons.id */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CouponIssuanceStatus status = CouponIssuanceStatus.AVAILABLE;

    @Column(name = "issued_at", updatable = false)
    private LocalDateTime issuedAt;

    /** 발급 자체의 만료 시각 (NULL 허용) */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** 이 발급이 사용된 예약(booking) id — 사용 전에는 NULL */
    @Column(name = "used_booking_id")
    private Long usedBookingId;
}
