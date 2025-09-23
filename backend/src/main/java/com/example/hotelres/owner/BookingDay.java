package com.example.hotelres.owner;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "booking_day",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_booking_day",
        columnNames = {"hotel_id", "room_type_id", "stay_date"}
    ),
    indexes = {
        @Index(name = "ix_bd_hotel_date", columnList = "hotel_id, stay_date"),
        @Index(name = "ix_bd_roomtype_date", columnList = "room_type_id, stay_date"),
        @Index(name = "ix_bd_sellable", columnList = "hotel_id, room_type_id, stay_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Column(name = "allotment", nullable = false)
    private int allotment = 0;  // 일자별 배정 객실 수

    @Column(name = "booked", nullable = false)
    private int booked = 0;     // 이미 예약된 수량

    @Column(name = "price", nullable = false)
    private int price = 0;      // 1박 가격(객실당)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BookingDayStatus status = BookingDayStatus.OPEN;

    // ====== DB 파생 컬럼 (읽기 전용) ======
    @Column(name = "remaining_qty", insertable = false, updatable = false)
    private Integer remainingQty;

    @Column(name = "is_sellable", insertable = false, updatable = false)
    private Boolean sellable;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // --- 편의 생성자 ---
    public BookingDay(Long hotelId, Long roomTypeId, LocalDate stayDate) {
        this.hotelId = hotelId;
        this.roomTypeId = roomTypeId;
        this.stayDate = stayDate;
        this.allotment = 0;
        this.booked = 0;
        this.price = 0;
        this.status = BookingDayStatus.OPEN;
    }
}
