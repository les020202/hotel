package com.example.hotelres.owner;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.hotelres.reservation.BookingGuest;

/**
 * 예약(Booking) 엔티티
 * - 실제 테이블: bookings
 * - 사용자(user_id), 호텔(hotel_id), 기간, 상태, 금액, 바우처 정보 포함
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK (users.id)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // FK (hotels.id)
    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private int nights;

    @Column(nullable = false)
    private int guests;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @Column(nullable = false, length = 3)
    private String currency = "KRW";

    @Column(name = "voucher_no", length = 40)
    private String voucherNo;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "canceled_by", length = 32) // USER / OWNER / ADMIN
    private String canceledBy;

    // --- 연관 관계 (예약 인원 정보) ---
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingGuest> guestsInfo = new ArrayList<>();
}
