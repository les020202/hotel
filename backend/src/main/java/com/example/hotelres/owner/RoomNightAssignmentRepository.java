// src/main/java/com/example/hotelres/owner/RoomNightAssignmentRepository.java
package com.example.hotelres.owner;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface RoomNightAssignmentRepository extends JpaRepository<RoomNightAssignmentEntity, Long> {

    // 대상 방-날짜에 활성 배정이 있으면 잠금으로 감지(충돌 방지)
    @Query(value = """
        SELECT 1 FROM room_night_assignments
         WHERE room_id = :roomId AND stay_date = :d AND released_at IS NULL
         LIMIT 1 FOR UPDATE
        """, nativeQuery = true)
    Integer lockRoomDate(@Param("roomId") Long roomId, @Param("d") LocalDate d);

    // 아이템-날짜 활성 배정 잠금 확인 (보통 release 후에는 없어야 함)
    @Query(value = """
        SELECT 1 FROM room_night_assignments
         WHERE booking_item_id = :itemId AND stay_date = :d AND released_at IS NULL
         LIMIT 1 FOR UPDATE
        """, nativeQuery = true)
    Integer lockItemDate(@Param("itemId") Long itemId, @Param("d") LocalDate d);

    // 활성 배정 개수
    @Query(value = """
        SELECT COUNT(*) FROM room_night_assignments
         WHERE room_id = :roomId AND stay_date = :d AND released_at IS NULL
        """, nativeQuery = true)
    int countActiveOnDate(@Param("roomId") Long roomId, @Param("d") LocalDate date);

    // 해당 날짜 방의 활성 배정 해제
    @Modifying
    @Query(value = """
        UPDATE room_night_assignments
           SET released_at = NOW()
         WHERE room_id = :roomId AND stay_date = :d AND released_at IS NULL
        """, nativeQuery = true)
    int releaseByDate(@Param("roomId") Long roomId, @Param("d") LocalDate date);

    // ✅ (핵심) 같은 booking_item_id + stay_date 이력(활성/해제 무관) 중 최신 1건 for update
    @Query(value = """
        SELECT id FROM room_night_assignments
         WHERE booking_item_id = :itemId AND stay_date = :d
         ORDER BY id DESC
         LIMIT 1 FOR UPDATE
        """, nativeQuery = true)
    Long findAnyIdByItemAndDateForUpdate(@Param("itemId") Long itemId, @Param("d") LocalDate date);

    // ✅ 재활성화: 기존 행을 새로운 room_id로 바꾸고 released_at=NULL
    @Modifying
    @Query(value = """
        UPDATE room_night_assignments
           SET room_id = :roomId,
               released_at = NULL
         WHERE id = :id
        """, nativeQuery = true)
    int reactivateByIdSetRoom(@Param("id") Long id, @Param("roomId") Long roomId);

    // 이 아이템의 활성 배정 전체 해제 (재배정 시작 전에 호출)
    @Modifying
    @Query(value = """
        UPDATE room_night_assignments
           SET released_at = NOW()
         WHERE booking_item_id = :itemId AND released_at IS NULL
        """, nativeQuery = true)
    int releaseAllByBookingItemId(@Param("itemId") Long itemId);

    // 필요시 사용(하드 삭제)
    @Modifying
    @Query(value = "DELETE FROM room_night_assignments WHERE booking_item_id = :itemId", nativeQuery = true)
    int deleteAllByBookingItemId(@Param("itemId") Long itemId);

    @Query(value = "SELECT COUNT(*) FROM room_night_assignments WHERE booking_item_id = :itemId", nativeQuery = true)
    int countByBookingItemId(@Param("itemId") Long itemId);
}
