package com.example.hotelres.owner.status;

import com.example.hotelres.owner.BookingDay; // ✅ JPA가 관리 중인 엔티티(아무거나 OK). 네이티브 쿼리라 엔티티는 타입만 채움.
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodayCheckInRepository extends JpaRepository<BookingDay, Long> {

    /**
     * 오늘 체크인 예정 건수 (일요일~토요일 기준 아님, '오늘' 하루 범위)
     * - check_in 이 DATE/DATETIME 모두 동작하도록 [start, end) 범위 비교
     * - 인덱스 권장: INDEX (hotel_id, check_in)
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM bookings
        WHERE hotel_id = :hotelId
          AND check_in >= :startDate
          AND check_in <  :endDate
        """, nativeQuery = true)
    long countToday(@Param("hotelId") Long hotelId,
                    @Param("startDate") LocalDate startDate,
                    @Param("endDate")   LocalDate endDate);
}
