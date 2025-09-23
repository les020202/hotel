// src/main/java/com/example/hotelres/reservation/BookingItem.java
package com.example.hotelres.reservation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booking_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)                // 부모 예약
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "rate_plan_id", nullable = false)
    private Long ratePlanId;

    @Column(name = "price_total", nullable = false)
    private Integer priceTotal;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;
}
