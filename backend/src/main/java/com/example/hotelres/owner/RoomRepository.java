// src/main/java/com/example/hotelres/owner/RoomRepository.java
package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.AvailableRoomDto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomRepository extends JpaRepository<RoomEntity, Long> {

    // 가용 객실 조회 (해제되지 않은 배정이 하나라도 있으면 제외)
    @Query(value = """
        SELECT
          r.id           AS roomId,
          r.room_no      AS roomNo,
          r.floor        AS floor,
          rt.type_code   AS typeCode,
          r.capacity     AS capacity,
          r.housekeeping AS housekeeping
        FROM rooms r
        JOIN room_types rt ON rt.id = r.room_type_id
        WHERE r.hotel_id = :hotelId
          AND r.status = 'ACTIVE'
          AND r.housekeeping IN ('CLEAN','INSPECTED')
          AND (
                (:upgrade = true AND
                  (CASE rt.type_code
                      WHEN 'STANDARD' THEN 1 WHEN 'DELUXE' THEN 2
                      WHEN 'PREMIUM' THEN 3 WHEN 'SUITE' THEN 4 ELSE 0 END)
                  >= (CASE :requestedType
                      WHEN 'STANDARD' THEN 1 WHEN 'DELUXE' THEN 2
                      WHEN 'PREMIUM' THEN 3 WHEN 'SUITE' THEN 4 ELSE 0 END)
                )
                OR
                (:upgrade = false AND rt.type_code = :requestedType)
              )
          AND r.capacity >= :minCapacity
          AND NOT EXISTS (
            SELECT 1
              FROM room_night_assignments a
             WHERE a.room_id   = r.id
               AND a.stay_date >= :from
               AND a.stay_date <  :to
               AND a.released_at IS NULL       -- ★ 해제된 배정만 점유로 간주
          )
        ORDER BY
          (CASE rt.type_code
              WHEN 'STANDARD' THEN 1 WHEN 'DELUXE' THEN 2
              WHEN 'PREMIUM' THEN 3 WHEN 'SUITE' THEN 4 ELSE 0 END),
          r.floor, r.room_no
        """, nativeQuery = true)
    List<Object[]> findAvailableRoomsNative(
            @Param("hotelId") Long hotelId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("minCapacity") int minCapacity,
            @Param("requestedType") String requestedType,
            @Param("upgrade") boolean upgrade
    );

    default List<AvailableRoomDto> toDtos(List<Object[]> rows) {
        return rows.stream().map(a -> new AvailableRoomDto(
                ((Number) a[0]).longValue(), // roomId
                (String) a[1],               // roomNo
                (Integer) a[2],              // floor
                (String) a[3],               // typeCode
                (Integer) a[4],              // capacity
                (String) a[5]                // housekeeping
        )).toList();
    }

    // JPQL에서는 엔티티명 사용 (RoomEntity)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RoomEntity r where r.id = :id")
    RoomEntity findByIdForUpdate(@Param("id") Long id);

    List<RoomEntity> findByHotelIdOrderByFloorAscRoomNoAsc(Long hotelId);
}
