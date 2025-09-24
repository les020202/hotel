// src/main/java/com/example/hotelres/reservation/MyBookingQueryRepository.java
package com.example.hotelres.reservation;

import com.example.hotelres.owner.BookingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.sql.Date;       // ← java.sql.Date (컨트롤러에서 LocalDate로 변환)
import java.util.Optional;

/**
 * "내 예약 요약" 네이티브 조회용 레포지토리.
 *
 * - 영수증 URL: payments.raw_payload(JSON)에 들어있는 $.receipt.url 에서 추출
 * - 날짜: DATE(b.check_in/out) 로 잘라 java.sql.Date 로 받음
 * - 상태값: b.status 는 문자열/enum String 저장 가정 (숫자면 쿼리/프로젝션을 맞춰주세요)
 *
 * 필요 테이블:
 *   bookings(id, user_id, hotel_id, check_in, check_out, nights, guests, total_amount, currency, status)
 *   booking_items(id, booking_id, room_type_id, ...)
 *   hotels(id, name, ...)
 *   room_types(id, name, ...)
 *   payments(id, booking_id, status, raw_payload, ...)
 */
public interface MyBookingQueryRepository extends Repository<BookingEntity, Long> {

    // 목록
    @Query(value = """
        SELECT
          b.id                               AS bookingId,
          CAST(b.status AS CHAR)             AS status,
          h.name                             AS hotelName,
          rt.name                            AS roomTypeName,
          DATE(b.check_in)                   AS checkIn,
          DATE(b.check_out)                  AS checkOut,
          b.nights                           AS nights,
          b.guests                           AS guests,
          b.total_amount                     AS totalAmount,
          b.currency                         AS currency,
          JSON_UNQUOTE(JSON_EXTRACT(p.raw_payload, '$.receipt.url')) AS receiptUrl
        FROM bookings b
        JOIN booking_items bi ON bi.booking_id = b.id
        JOIN hotels       h   ON h.id = b.hotel_id
        JOIN room_types   rt  ON rt.id = bi.room_type_id
        LEFT JOIN payments p  ON p.id = (
            SELECT p2.id
            FROM payments p2
            WHERE p2.booking_id = b.id
              AND p2.status = 'SUCCEEDED'  -- 숫자(enum ordinal)면 해당 값으로 교체
            ORDER BY p2.id DESC
            LIMIT 1
        )
        WHERE b.user_id = :userId
        ORDER BY b.id DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM bookings b
        WHERE b.user_id = :userId
        """,
            nativeQuery = true)
    Page<MyBookingRow> findMyBookings(@Param("userId") Long userId, Pageable pageable);

    // 단건
    @Query(value = """
        SELECT
          b.id                               AS bookingId,
          CAST(b.status AS CHAR)             AS status,
          h.name                             AS hotelName,
          rt.name                            AS roomTypeName,
          DATE(b.check_in)                   AS checkIn,
          DATE(b.check_out)                  AS checkOut,
          b.nights                           AS nights,
          b.guests                           AS guests,
          b.total_amount                     AS totalAmount,
          b.currency                         AS currency,
          JSON_UNQUOTE(JSON_EXTRACT(p.raw_payload, '$.receipt.url')) AS receiptUrl
        FROM bookings b
        JOIN booking_items bi ON bi.booking_id = b.id
        JOIN hotels       h   ON h.id = b.hotel_id
        JOIN room_types   rt  ON rt.id = bi.room_type_id
        LEFT JOIN payments p  ON p.id = (
            SELECT p2.id
            FROM payments p2
            WHERE p2.booking_id = b.id
              AND p2.status = 'SUCCEEDED'  -- 숫자(enum ordinal)면 해당 값으로 교체
            ORDER BY p2.id DESC
            LIMIT 1
        )
        WHERE b.user_id = :userId
          AND b.id      = :bookingId
        """,
            nativeQuery = true)
    Optional<MyBookingRow> findMyBooking(@Param("userId") Long userId,
                                         @Param("bookingId") Long bookingId);

    /**
     * 네이티브 결과 매핑용 프로젝션.
     * 메서드명은 SELECT 별칭과 1:1로 매칭되어야 합니다.
     */
    interface MyBookingRow {
        Long   getBookingId();
        String getStatus();
        String getHotelName();
        String getRoomTypeName();
        Date   getCheckIn();     // java.sql.Date
        Date   getCheckOut();    // java.sql.Date
        Integer getNights();
        Integer getGuests();
        Integer getTotalAmount();
        String getCurrency();
        String getReceiptUrl();  // JSON에서 추출
    }
}
