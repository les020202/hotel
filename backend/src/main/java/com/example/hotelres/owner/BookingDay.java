package com.example.hotelres.owner;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "booking_day",
        uniqueConstraints = @UniqueConstraint(name="uk_booking_day", columnNames = {"hotel_id","room_type_id","stay_date"}),
        indexes = {
                @Index(name="ix_bd_hotel_date", columnList="hotel_id, stay_date"),
                @Index(name="ix_bd_roomtype_date", columnList="room_type_id, stay_date"),
                @Index(name="ix_bd_sellable", columnList="hotel_id, room_type_id, stay_date")
        })
public class BookingDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="hotel_id", nullable=false)
    private Long hotelId;

    @Column(name="room_type_id", nullable=false)
    private Long roomTypeId;

    @Column(name="stay_date", nullable=false)
    private LocalDate stayDate;

    @Column(name="allotment", nullable=false)
    private int allotment;

    @Column(name="booked", nullable=false)
    private int booked;

    @Column(name="price", nullable=false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false, length=16)
    private BookingDayStatus status = BookingDayStatus.OPEN;

    // 생성(가상) 컬럼: insert/update 대상 아님
    @Column(name="remaining_qty", insertable=false, updatable=false)
    private Integer remainingQty;

    @Column(name="is_sellable", insertable=false, updatable=false)
    private Boolean sellable;

    @Column(name="created_at", insertable=false, updatable=false)
    private java.time.LocalDateTime createdAt;

    @Column(name="updated_at", insertable=false, updatable=false)
    private java.time.LocalDateTime updatedAt;

    // --- ctor ---
    protected BookingDay() {}
    public BookingDay(Long hotelId, Long roomTypeId, LocalDate stayDate) {
        this.hotelId = hotelId;
        this.roomTypeId = roomTypeId;
        this.stayDate = stayDate;
        this.allotment = 0;
        this.booked = 0;
        this.price = 0;
        this.status = BookingDayStatus.OPEN;
    }

    // --- getters/setters ---
    public Long getId() { return id; }
    public Long getHotelId() { return hotelId; }
    public Long getRoomTypeId() { return roomTypeId; }
    public LocalDate getStayDate() { return stayDate; }
    public int getAllotment() { return allotment; }
    public void setAllotment(int allotment) { this.allotment = allotment; }
    public int getBooked() { return booked; }
    public void setBooked(int booked) { this.booked = booked; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public BookingDayStatus getStatus() { return status; }
    public void setStatus(BookingDayStatus status) { this.status = status; }
    public Integer getRemainingQty() { return remainingQty; }
    public Boolean getSellable() { return sellable; }
}
