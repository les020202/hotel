// src/main/java/com/example/hotelres/owner/RoomNightAssignmentEntity.java
package com.example.hotelres.owner;
import jakarta.persistence.*; import java.time.LocalDate; import java.time.LocalDateTime;

@Entity @Table(name="room_night_assignments",
        uniqueConstraints = {
                @UniqueConstraint(name="uk_room_date", columnNames={"room_id","stay_date"}),
                @UniqueConstraint(name="uk_item_date", columnNames={"booking_item_id","stay_date"})
        })
public class RoomNightAssignmentEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="room_id") Long roomId;
    @Column(name="booking_item_id") Long bookingItemId;
    @Column(name="stay_date") LocalDate stayDate;
    @Column(name="assigned_at", insertable=false, updatable=false) LocalDateTime assignedAt;
    public RoomNightAssignmentEntity() {}
    public RoomNightAssignmentEntity(Long roomId, Long bookingItemId, LocalDate stayDate){
        this.roomId=roomId; this.bookingItemId=bookingItemId; this.stayDate=stayDate;
    }
}
