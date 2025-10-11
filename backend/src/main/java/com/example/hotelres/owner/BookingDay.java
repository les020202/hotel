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
                @Index(name = "ix_bd_hotel_date",    columnList = "hotel_id, stay_date"),
                @Index(name = "ix_bd_roomtype_date", columnList = "room_type_id, stay_date")
                // ⚠ is_sellable, remaining_qty 는 가상 컬럼이라 JPA @Index에 못 넣습니다. DB DDL에서만 유지하세요.
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
    private BookingDayStatus status = BookingDayStatus.OPEN; // OPEN/CLOSED/SOLD_OUT

    // ---- DB 생성(가상) 컬럼: 읽기 전용 ----
    @Column(name = "remaining_qty", insertable = false, updatable = false)
    private Integer remainingQty;

    @Column(name = "is_sellable", insertable = false, updatable = false)
    private Boolean sellable;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // (선택) 낙관적 락 – 현재는 PESSIMISTIC_WRITE를 쓰지만, 추가해도 무방
    // @Version
    // private Long version;

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

    // ===== 도메인 편의 메서드 =====

    /** 예약 발생: booked += qty (allotment 초과 방지) */
    public void book(int qty) {
        if (qty <= 0) return;
        int next = this.booked + qty;
        if (next > this.allotment) {
            // 정책에 따라 예외를 던지거나 최대치로 clamp
            next = this.allotment;
        }
        this.booked = next;
        soldOutIfNeeded();
    }

    /** 예약 취소: booked -= qty (0 미만 방지) */
    public void release(int qty) {
        if (qty <= 0) return;
        int next = this.booked - qty;
        if (next < 0) next = 0;
        this.booked = next;
        // 매진 해제
        if (this.status == BookingDayStatus.SOLD_OUT && this.booked < this.allotment) {
            this.status = BookingDayStatus.OPEN;
        }
    }

    /** 현재 수치에 맞춰 SOLD_OUT/OPEN 보정 */
    public void soldOutIfNeeded() {
        if (this.booked >= this.allotment && this.allotment > 0) {
            this.status = BookingDayStatus.SOLD_OUT;
        } else if (this.status == BookingDayStatus.SOLD_OUT && this.booked < this.allotment) {
            this.status = BookingDayStatus.OPEN;
        }
    }
}
