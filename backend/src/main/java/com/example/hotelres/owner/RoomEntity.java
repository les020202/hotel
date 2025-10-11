package com.example.hotelres.owner;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class RoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "room_no", nullable = false, length = 20)
    private String roomNo;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "building", length = 50)
    private String building;

    // ACTIVE / INACTIVE
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status = RoomStatus.ACTIVE;

    // CLEAN / DIRTY / INSPECTED / OCCUPIED
    @Enumerated(EnumType.STRING)
    @Column(name = "housekeeping", nullable = false)
    private HousekeepingStatus housekeeping = HousekeepingStatus.CLEAN;

    @Column(name = "capacity")
    private Integer capacity;

    // --- getters ---
    public Long getId() { return id; }
    public Long getHotelId() { return hotelId; }
    public Long getRoomTypeId() { return roomTypeId; }
    public String getRoomNo() { return roomNo; }
    public Integer getFloor() { return floor; }
    public String getBuilding() { return building; }
    public RoomStatus getStatus() { return status; }
    public HousekeepingStatus getHousekeeping() { return housekeeping; }
    public Integer getCapacity() { return capacity; }

    // --- setters ---
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }
    public void setRoomTypeId(Long roomTypeId) { this.roomTypeId = roomTypeId; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
    public void setFloor(Integer floor) { this.floor = floor; }
    public void setBuilding(String building) { this.building = building; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public void setHousekeeping(HousekeepingStatus housekeeping) { this.housekeeping = housekeeping; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}
