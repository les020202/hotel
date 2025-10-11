// src/main/java/com/example/hotelres/owner/BookingDayRepository.java
package com.example.hotelres.owner;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingDayRepository extends JpaRepository<BookingDay, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT bd FROM BookingDay bd
         WHERE bd.hotelId    = :hotelId
           AND bd.roomTypeId = :roomTypeId
           AND bd.stayDate  >= :checkIn
           AND bd.stayDate  <  :checkOut
         ORDER BY bd.stayDate
    """)
    List<BookingDay> findForUpdate(
            @Param("hotelId") Long hotelId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT bd FROM BookingDay bd
         WHERE bd.hotelId    = :hotelId
           AND bd.roomTypeId IN :roomTypeIds
           AND bd.stayDate  >= :checkIn
           AND bd.stayDate  <  :checkOut
         ORDER BY bd.roomTypeId, bd.stayDate
    """)
    List<BookingDay> findForUpdateIn(
            @Param("hotelId") Long hotelId,
            @Param("roomTypeIds") List<Long> roomTypeIds,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    @Query("""
        SELECT bd FROM BookingDay bd
         WHERE bd.hotelId    = :hotelId
           AND bd.roomTypeId = :roomTypeId
           AND bd.stayDate  >= :checkIn
           AND bd.stayDate  <  :checkOut
         ORDER BY bd.stayDate
    """)
    List<BookingDay> findRange(
            @Param("hotelId") Long hotelId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    /* 취소 시 재고 복구: booked 감소 (+ status가 SOLD_OUT이면 재고 생긴 경우 OPEN으로) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE booking_day
       SET booked = GREATEST(booked - :delta, 0),
           status = CASE
                      WHEN (allotment - GREATEST(booked - :delta, 0)) > 0
                           AND status <> 'CLOSED' THEN 'OPEN'
                      ELSE status
                    END
     WHERE hotel_id     = :hotelId
       AND room_type_id = :roomTypeId
       AND stay_date   >= :checkIn
       AND stay_date   <  :checkOut
""", nativeQuery = true)
    int restoreInventory(
            @Param("hotelId") Long hotelId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") java.time.LocalDate checkIn,
            @Param("checkOut") java.time.LocalDate checkOut,
            @Param("delta") int delta
    );
}
