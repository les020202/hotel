// src/main/java/com/example/hotelres/owner/RoomNightAssignmentRepository.java
package com.example.hotelres.owner;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface RoomNightAssignmentRepository extends JpaRepository<RoomNightAssignmentEntity, Long> {

    @Query(value = "select 1 from room_night_assignments where room_id=:roomId and stay_date=:d limit 1 for update", nativeQuery = true)
    Integer lockRoomDate(@Param("roomId") Long roomId, @Param("d") LocalDate d);

    @Query(value = "select 1 from room_night_assignments where booking_item_id=:itemId and stay_date=:d limit 1 for update", nativeQuery = true)
    Integer lockItemDate(@Param("itemId") Long itemId, @Param("d") LocalDate d);

    @Modifying
    @Query(value = "delete from room_night_assignments where booking_item_id=:itemId", nativeQuery = true)
    int deleteAllByBookingItemId(@Param("itemId") Long itemId);

    @Query(value = "select count(*) from room_night_assignments where booking_item_id=:itemId", nativeQuery = true)
    int countByBookingItemId(@Param("itemId") Long itemId);
}
