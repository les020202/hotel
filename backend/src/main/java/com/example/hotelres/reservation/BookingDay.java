// path: src/main/java/com/example/hotelres/reservation/BookingDay.java
package com.example.hotelres.reservation;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "booking_day") // UNIQUE(hotel_id, room_type_id, stay_date)
@Getter @Setter
@NoArgsConstructor
public class BookingDay {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Column(name = "allotment", nullable = false)
    private Integer allotment; // 일자별 배정 객실 수

    @Column(name = "booked", nullable = false)
    private Integer booked;    // 이미 예약된 수량

    @Column(name = "price", nullable = false)
    private Integer price;     // 1박 가격(객실당)

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BookingDayStatus status = BookingDayStatus.OPEN;

    // ====== DB 파생컬럼 매핑 (읽기 전용) ======
    @Column(name = "remaining_qty", insertable = false, updatable = false)
    private Integer remainingQty; // allotment - booked

    @Column(name = "is_sellable", insertable = false, updatable = false)
    private Boolean isSellable;   // status=OPEN && remaining_qty>0

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private java.time.LocalDateTime updatedAt;
}
