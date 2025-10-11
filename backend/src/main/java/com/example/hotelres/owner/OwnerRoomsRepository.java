// path: backend/src/main/java/com/example/hotelres/owner/OwnerRoomsRepository.java
package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.RoomStatusDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OwnerRoomsRepository extends JpaRepository<RoomEntity, Long> {

    // 객실 현황: 해제되지 않은 배정만 점유로 계산 (a.releasedAt is null)
    @Query("""
      select new com.example.hotelres.owner.dto.RoomStatusDto(
        r.id, r.roomNo, r.floor,
        rt.id, rt.typeCode, rt.name,
        r.capacity, r.status, r.housekeeping,
        case when a.id is not null then true else false end
      )
      from RoomEntity r
        join RoomTypeEntity rt on rt.id = r.roomTypeId
        left join RoomNightAssignmentEntity a
          on a.roomId = r.id
         and a.stayDate = :date
         and a.releasedAt is null
      where r.hotelId = :hotelId
      order by r.floor asc, r.roomNo asc
    """)
    List<RoomStatusDto> findStatus(@Param("hotelId") Long hotelId,
                                   @Param("date") LocalDate date);

    // ─────────────────────────────────────────────────────────
    // 방 ID별 대표 투숙객(첫 번째) 네이티브 조회
    // ─────────────────────────────────────────────────────────
    interface RoomGuestProjection {
        Long   getRoomId();
        String getGuestName();
        String getGuestPhone();
    }

    @Query(value = """
        SELECT
          r.id AS roomId,
          (SELECT g.name
             FROM booking_guests g
            WHERE g.booking_id = b.id
            ORDER BY g.id ASC
            LIMIT 1) AS guestName,
          (SELECT g.phone
             FROM booking_guests g
            WHERE g.booking_id = b.id
            ORDER BY g.id ASC
            LIMIT 1) AS guestPhone
        FROM rooms r
        LEFT JOIN room_night_assignments a
          ON a.room_id   = r.id
         AND a.stay_date = :date
         AND a.released_at IS NULL
        LEFT JOIN booking_items bi
          ON bi.id = a.booking_item_id
        LEFT JOIN bookings b
          ON b.id = bi.booking_id
         AND b.status = 'CONFIRMED'
        WHERE r.hotel_id = :hotelId
    """, nativeQuery = true)
    List<RoomGuestProjection> findGuestByRoomOnDate(@Param("hotelId") Long hotelId,
                                                    @Param("date") LocalDate date);
}
