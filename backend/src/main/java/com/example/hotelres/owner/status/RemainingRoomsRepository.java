package com.example.hotelres.owner.status;

import com.example.hotelres.owner.BookingDay;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RemainingRoomsRepository extends JpaRepository<BookingDay, Long> {

    /**
     * 오늘 잔여 객실 합계
     * - stay_date : DATE 컬럼
     * - remaining_qty 가 null(가상/파생 계산 안 됐을 때) 대비해 allotment - booked 로 대체
     */
    @Query(value = """
        SELECT COALESCE(
                 SUM(IFNULL(remaining_qty, GREATEST(allotment - booked, 0))),
                 0
               )
        FROM booking_day
        WHERE hotel_id = :hotelId
          AND stay_date = :date
        """, nativeQuery = true)
    long sumTodayRemaining(@Param("hotelId") Long hotelId,
                           @Param("date") LocalDate date);
}
