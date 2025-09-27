package com.example.hotelres.owner.status;

import com.example.hotelres.owner.BookingDay; // ✅ 관리되는 엔티티
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReserveThisWeekRepository extends JpaRepository<BookingDay, Long> { // ✅ Object → BookingDay

    @Query(value = """
        SELECT COUNT(*)
        FROM bookings
        WHERE hotel_id = :hotelId
          AND check_in >= (CURRENT_DATE - INTERVAL (DAYOFWEEK(CURRENT_DATE) - 1) DAY)
          AND check_in <  DATE_ADD( (CURRENT_DATE - INTERVAL (DAYOFWEEK(CURRENT_DATE) - 1) DAY), INTERVAL 7 DAY )
        """, nativeQuery = true)
    long countThisWeek(@Param("hotelId") Long hotelId);
}
