// path: src/main/java/com/example/hotelres/reservation/BookingDayRepository.java
package com.example.hotelres.reservation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingDayRepository extends JpaRepository<BookingDay, Long> {

    /** 재고 차감/홀드 시 사용: 범위를 PESSIMISTIC_WRITE로 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT bd FROM BookingDay bd
         WHERE bd.hotelId    = :hotelId
           AND bd.roomTypeId = :roomTypeId
           AND bd.stayDate  >= :checkIn
           AND bd.stayDate  <  :checkOut
         ORDER BY bd.stayDate
    """)
    List<BookingDay> findForUpdate(@Param("hotelId") Long hotelId,
                                   @Param("roomTypeId") Long roomTypeId,
                                   @Param("checkIn") LocalDate checkIn,
                                   @Param("checkOut") LocalDate checkOut);
}
