package com.example.hotelres.owner.status;

import com.example.hotelres.owner.BookingDay; // JPA 관리 엔티티(네이티브용 타입 채우기)
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NowCheckInRepository extends JpaRepository<BookingDay, Long> {

    /**
     * 금일 체크인 완료 인원(중복 방지: booking_item_id 기준 DISTINCT)
     * - assigned_at : 오늘 범위 [start, end)
     * - rooms 조인으로 호텔 필터
     */
    @Query(value = """
        SELECT COUNT(DISTINCT rna.booking_item_id)
        FROM room_night_assignments rna
        JOIN rooms r ON r.id = rna.room_id
        WHERE r.hotel_id = :hotelId
          AND rna.assigned_at >= :start
          AND rna.assigned_at <  :end
        """, nativeQuery = true)
    long countTodayCheckedInDistinct(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end
    );
}
