package com.example.hotelres.owner;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    @Query("""
      select new com.example.hotelres.owner.OwnerBookingDto(
        b.id, b.checkIn, b.checkOut, b.nights, b.guests, b.totalAmount, cast(b.status as string), b.voucherNo
      )
      from BookingEntity b
      where b.hotelId = :hotelId
        and b.checkIn >= :from and b.checkOut <= :to
        and (:status is null or cast(b.status as string) = :status)
        and (:q is null or :q = '' or lower(b.voucherNo) like concat('%', lower(:q), '%'))
      order by b.createdAt desc
    """)
    Page<OwnerBookingDto> findOwnerBookings(
            @Param("hotelId") Long hotelId,
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to,
            @Param("status") String status,   // "CONFIRMED" 등 문자열로 전달
            @Param("q") String q,
            Pageable pageable
    );
}
