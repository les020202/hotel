package com.example.hotelres.owner;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;

public interface BookingDayRepository extends JpaRepository<BookingDay, Long> {

    @Query("""
        select b from BookingDay b
        where b.hotelId = :hotelId
          and b.roomTypeId = :roomTypeId
          and b.stayDate between :from and :to
        order by b.stayDate
    """)
    List<BookingDay> findRange(@Param("hotelId") Long hotelId,
                               @Param("roomTypeId") Long roomTypeId,
                               @Param("from") LocalDate from,
                               @Param("to") LocalDate to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b from BookingDay b
        where b.hotelId = :hotelId and b.roomTypeId = :roomTypeId and b.stayDate = :day
    """)
    Optional<BookingDay> findForUpdate(@Param("hotelId") Long hotelId,
                                       @Param("roomTypeId") Long roomTypeId,
                                       @Param("day") LocalDate day);
}
