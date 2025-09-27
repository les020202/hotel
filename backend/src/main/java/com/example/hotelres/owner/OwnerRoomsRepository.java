// path: backend/src/main/java/com/example/hotelres/owner/OwnerRoomsRepository.java
package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.RoomStatusDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OwnerRoomsRepository extends JpaRepository<RoomEntity, Long> {

    // ✅ 객실 현황: 게스트는 포함하지 않고, 배정여부(occupied-equivalent)만 JPQL로 반환
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
          on a.roomId = r.id and a.stayDate = :date
      where r.hotelId = :hotelId
      order by r.floor asc, r.roomNo asc
    """)
    List<RoomStatusDto> findStatus(@Param("hotelId") Long hotelId,
                                   @Param("date") LocalDate date);

    // ─────────────────────────────────────────────────────────
    // ✅ 방 ID별 대표 투숙객(첫 번째) 네이티브 조회
    //   - room_night_assignments → booking_items → bookings → booking_guests
    //   - alias 를 camelCase 로 맞춰서 interface 기반 프로젝션에 정확히 매핑
    //   - 대표 투숙객: booking_guests 의 "첫 행" (id ASC 기준)
    // ─────────────────────────────────────────────────────────
    interface RoomGuestProjection {
        Long   getRoomId();
        String getGuestName();
        String getGuestPhone();
    }

    @Query(value = """
        SELECT
          r.id AS roomId,
          /* 대표 투숙객(첫 번째)만 추출 */
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
        LEFT JOIN booking_items bi
          ON bi.id = a.booking_item_id           -- ★ 핵심: booking_item_id로 연결
        LEFT JOIN bookings b
          ON b.id = bi.booking_id
         AND b.status = 'CONFIRMED'              -- enum이 문자열 저장이면 OK (숫자면 값에 맞게 변경)
        WHERE r.hotel_id = :hotelId
    """, nativeQuery = true)
    List<RoomGuestProjection> findGuestByRoomOnDate(@Param("hotelId") Long hotelId,
                                                    @Param("date") LocalDate date);
}
