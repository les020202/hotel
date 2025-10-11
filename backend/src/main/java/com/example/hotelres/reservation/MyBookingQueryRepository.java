package com.example.hotelres.reservation;

import com.example.hotelres.owner.BookingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.sql.Date;

public interface MyBookingQueryRepository extends Repository<BookingEntity, Long> {

    /* 목록 */
    @Query(value = """
        SELECT
          b.id                               AS bookingId,
          CAST(b.status AS CHAR)             AS status,
          b.hotel_id                         AS hotelId,     -- ✅ 추가
          h.name                             AS hotelName,
          rt.name                            AS roomTypeName,
          DATE(b.check_in)                   AS checkIn,
          DATE(b.check_out)                  AS checkOut,
          b.nights                           AS nights,
          b.guests                           AS guests,
          b.total_amount                     AS totalAmount,
          b.currency                         AS currency,
          JSON_UNQUOTE(JSON_EXTRACT(p.raw_payload, '$.receipt.url')) AS receiptUrl,
          /* ✅ 대표 투숙객 1명(첫 번째) */
          (SELECT g.name  FROM booking_guests g WHERE g.booking_id = b.id ORDER BY g.id ASC LIMIT 1) AS guestName,
          (SELECT g.phone FROM booking_guests g WHERE g.booking_id = b.id ORDER BY g.id ASC LIMIT 1) AS guestPhone,
          /* 취소 메타 */
          DATE_FORMAT(b.canceled_at, '%Y-%m-%dT%H:%i:%s') AS canceledAt,
          b.canceled_by                      AS canceledBy,
          b.cancel_reason                    AS cancelReason
        FROM bookings b
        JOIN booking_items bi ON bi.booking_id = b.id
        JOIN hotels       h   ON h.id = b.hotel_id
        JOIN room_types   rt  ON rt.id = bi.room_type_id
        LEFT JOIN payments p  ON p.id = (
            SELECT p2.id
            FROM payments p2
            WHERE p2.booking_id = b.id
              AND p2.status = 'SUCCEEDED'
            ORDER BY p2.id DESC
            LIMIT 1
        )
        WHERE b.user_id = :userId
        ORDER BY b.id DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM bookings b WHERE b.user_id = :userId
        """,
        nativeQuery = true)
    Page<MyBookingRow> findMyBookings(@Param("userId") Long userId, Pageable pageable);

    /* 단건 */
    @Query(value = """
        SELECT
          b.id                               AS bookingId,
          CAST(b.status AS CHAR)             AS status,
          b.hotel_id                         AS hotelId,     -- ✅ 추가
          h.name                             AS hotelName,
          rt.name                            AS roomTypeName,
          DATE(b.check_in)                   AS checkIn,
          DATE(b.check_out)                  AS checkOut,
          b.nights                           AS nights,
          b.guests                           AS guests,
          b.total_amount                     AS totalAmount,
          b.currency                         AS currency,
          JSON_UNQUOTE(JSON_EXTRACT(p.raw_payload, '$.receipt.url')) AS receiptUrl,
          /* ✅ 대표 투숙객 1명(첫 번째) */
          (SELECT g.name  FROM booking_guests g WHERE g.booking_id = b.id ORDER BY g.id ASC LIMIT 1) AS guestName,
          (SELECT g.phone FROM booking_guests g WHERE g.booking_id = b.id ORDER BY g.id ASC LIMIT 1) AS guestPhone,
          /* 취소 메타 */
          DATE_FORMAT(b.canceled_at, '%Y-%m-%dT%H:%i:%s') AS canceledAt,
          b.canceled_by                      AS canceledBy,
          b.cancel_reason                    AS cancelReason
        FROM bookings b
        JOIN booking_items bi ON bi.booking_id = b.id
        JOIN hotels       h   ON h.id = b.hotel_id
        JOIN room_types   rt  ON rt.id = bi.room_type_id
        LEFT JOIN payments p  ON p.id = (
            SELECT p2.id
            FROM payments p2
            WHERE p2.booking_id = b.id
              AND p2.status = 'SUCCEEDED'
            ORDER BY p2.id DESC
            LIMIT 1
        )
        WHERE b.user_id = :userId
          AND b.id      = :bookingId
        """,
        nativeQuery = true)
    java.util.Optional<MyBookingRow> findMyBooking(@Param("userId") Long userId,
                                                   @Param("bookingId") Long bookingId);

    /** 네이티브 결과 매핑용 프로젝션 (SELECT 별칭과 1:1) */
    interface MyBookingRow {
        Long    getBookingId();
        String  getStatus();
        Long    getHotelId();     // ✅ 추가
        String  getHotelName();
        String  getRoomTypeName();
        Date    getCheckIn();
        Date    getCheckOut();
        Integer getNights();
        Integer getGuests();
        Integer getTotalAmount();
        String  getCurrency();
        String  getReceiptUrl();

        String  getGuestName();
        String  getGuestPhone();

        String  getCanceledAt();
        String  getCanceledBy();
        String  getCancelReason();
    }
}
