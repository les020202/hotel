package com.example.hotelres.owner;

import jakarta.persistence.*;

@Entity
@Table(name = "room_types")
public class RoomTypeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="hotel_id") private Long hotelId;
    @Column private String name;
    @Column(name="type_code") private String typeCode;
    public Long getId(){return id;} public Long getHotelId(){return hotelId;}
    public String getName(){return name;} public String getTypeCode(){return typeCode;}
}
