// src/main/java/com/example/hotelres/owner/BookingRepository.java
package com.example.hotelres.owner;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    @Query("""
      select new com.example.hotelres.owner.OwnerBookingDto(
        b.id,
        b.checkIn,
        b.checkOut,
        b.nights,
        b.guests,
        b.totalAmount,
        concat('', b.status),
        b.voucherNo
      )
      from BookingEntity b
      where b.hotelId = :hotelId
        and (b.checkIn <= :to and b.checkOut >= :from)
        and (:status is null or :status = '' or concat('', b.status) = :status)
        and (:q is null or :q = '' or lower(b.voucherNo) like concat('%', lower(:q), '%'))
      order by b.id desc
    """)
    Page<OwnerBookingDto> findOwnerBookings(
            @Param("hotelId") Long hotelId,
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to,
            @Param("status") String status,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
      select b from BookingEntity b
      where b.userId = :userId
        and b.hotelId = :hotelId
        and b.checkOut <= :today
      order by b.id desc
    """)
    List<BookingEntity> findPastBookingsForReview(
            @Param("userId") Long userId,
            @Param("hotelId") Long hotelId,
            @Param("today") LocalDate today
    );

    boolean existsByIdAndHotelId(Long id, Long hotelId);

    /* ----------------------- 추가: 룸타입명 조회 ----------------------- */

    /**
     * 예약의 대표 객실타입명 1개를 가져옵니다.
     * booking_items(room_type_id) → room_types(name) 조인.
     */
    @Query(value = """
        SELECT rt.name
        FROM booking_items bi
        JOIN room_types   rt ON rt.id = bi.room_type_id
        WHERE bi.booking_id = :bookingId
        LIMIT 1
        """, nativeQuery = true)
    String findRoomTypeNameByBookingId(@Param("bookingId") Long bookingId);

    // (옵션) 여러 객실타입을 합쳐서 보고 싶다면 주석 해제해서 사용하세요.
    // @Query(value = """
    //     SELECT GROUP_CONCAT(rt.name SEPARATOR ', ')
    //     FROM booking_items bi
    //     JOIN room_types rt ON rt.id = bi.room_type_id
    //     WHERE bi.booking_id = :bookingId
    //     """, nativeQuery = true)
    // String findRoomTypeNamesByBookingId(@Param("bookingId") Long bookingId);
}
