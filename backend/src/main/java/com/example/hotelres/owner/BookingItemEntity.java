package com.example.hotelres.owner;
import jakarta.persistence.*;

@Entity @Table(name="booking_items")
public class BookingItemEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="booking_id") Long bookingId;
    @Column(name="room_type_id") Long roomTypeId;
    @Column Integer quantity; // 1권장
    public Long getId(){return id;} public Long getBookingId(){return bookingId;}
    public Long getRoomTypeId(){return roomTypeId;}
}
