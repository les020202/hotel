package com.example.hotelres.main.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.hotelres.hotel.Hotel;

@Getter @Setter
@Entity
@Table(name = "booking_day")
public class BookingDayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 호텔 FK */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    /** 객실타입 ID (필요시 사용) */
    @Column(name = "room_type_id")
    private Long roomTypeId;

    /** 숙박 일자 */
    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    /** 총 배정 수량 */
    @Column(name = "allotment")
    private Integer allotment;

    /** 이미 예약된 수량 */
    @Column(name = "booked")
    private Integer booked;

    /** 1박 가격(KRW) */
    @Column(name = "price", nullable = false)
    private Integer price;

    /** 판매 상태 (ENUM('OPEN','CLOSE'…)) – 실제 DB 값에 맞게 사용 */
    public enum Status { OPEN, CLOSE }
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.OPEN;

    /** 남은 수량 (가상 컬럼) */
    @Column(name = "remaining_qty", insertable = false, updatable = false)
    private Integer remainingQty;

    /** 판매 가능 여부 (TINYINT(1)) */
    @Column(name = "is_sellable")
    private Boolean isSellable;

    /** 생성/수정 시각 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
