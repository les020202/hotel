// path: src/main/java/com/example/hotelres/reservation/BookingHold.java
package com.example.hotelres.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booking_holds") // 실제 테이블명
@Getter @Setter
@NoArgsConstructor
public class BookingHold {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hold_code", nullable = false, length = 40, unique = true)
    private String holdCode;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "rate_plan_id", nullable = false)
    private Long ratePlanId;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private int guests;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "room_subtotal", nullable = false)
    private int roomSubtotal;

    @Column(nullable = false)
    private int discount = 0;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @Column(nullable = false)
    private String currency = "KRW";

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (currency == null) currency = "KRW";
        if (expiresAt == null) expiresAt = LocalDateTime.now().plusMinutes(15);
        if (holdCode == null)  holdCode  = "HLD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
