// src/main/java/com/example/hotelres/admin/booking/AdminBookingQueryRepository.java
package com.example.hotelres.admin.booking;

import com.example.hotelres.owner.BookingEntity; // ★ 실제 엔티티
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.time.LocalDate;

public interface AdminBookingQueryRepository extends Repository<BookingEntity, Long> {

    @Query(
            value = """
      SELECT
        b.id                                  AS bookingId,
        CAST(b.status AS CHAR)                AS status,
        h.name                                AS hotelName,
        rt.name                               AS roomTypeName,
        DATE(b.check_in)                      AS checkIn,
        DATE(b.check_out)                     AS checkOut,
        b.nights                              AS nights,
        b.guests                              AS guests,
        b.total_amount                        AS totalAmount,
        b.currency                            AS currency,
        u.id                                  AS userId,
        u.login_id                            AS userLoginId,
        u.name                                AS userName,
        JSON_UNQUOTE(JSON_EXTRACT(p.raw_payload,'$.receipt.url')) AS receiptUrl
      FROM bookings b
      JOIN users u           ON u.id = b.user_id
      JOIN booking_items bi  ON bi.booking_id = b.id
      JOIN hotels h          ON h.id = b.hotel_id
      JOIN room_types rt     ON rt.id = bi.room_type_id
      LEFT JOIN payments p   ON p.id = (
          SELECT p2.id
          FROM payments p2
          WHERE p2.booking_id = b.id
            AND p2.status = 'SUCCEEDED'     -- 숫자로 저장이면 값으로 교체
          ORDER BY p2.id DESC
          LIMIT 1
      )
      WHERE
        (:status     IS NULL OR b.status = :status)
        AND (:hotelId   IS NULL OR b.hotel_id = :hotelId)
        AND (:hotelName IS NULL OR h.name LIKE CONCAT('%', :hotelName, '%'))   -- ★ 호텔명 필터 추가
        AND (:loginId   IS NULL OR u.login_id LIKE CONCAT('%', :loginId, '%'))
        AND (:from      IS NULL OR DATE(b.check_in) >= :from)
        AND (:to        IS NULL OR DATE(b.check_in) <= :to)
      ORDER BY b.id DESC
      """,
            countQuery = """
      SELECT COUNT(*)
      FROM bookings b
      JOIN users u           ON u.id = b.user_id
      JOIN booking_items bi  ON bi.booking_id = b.id
      JOIN hotels h          ON h.id = b.hotel_id       -- ★ count에서도 h 조인
      JOIN room_types rt     ON rt.id = bi.room_type_id
      WHERE
        (:status     IS NULL OR b.status = :status)
        AND (:hotelId   IS NULL OR b.hotel_id = :hotelId)
        AND (:hotelName IS NULL OR h.name LIKE CONCAT('%', :hotelName, '%'))   -- ★ 동일 조건
        AND (:loginId   IS NULL OR u.login_id LIKE CONCAT('%', :loginId, '%'))
        AND (:from      IS NULL OR DATE(b.check_in) >= :from)
        AND (:to        IS NULL OR DATE(b.check_in) <= :to)
      """,
            nativeQuery = true
    )
    Page<AdminBookingRow> findAdminRows(@Param("status") String status,
                                        @Param("hotelId") Long hotelId,
                                        @Param("hotelName") String hotelName,   // ★ 추가
                                        @Param("loginId") String loginId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to,
                                        Pageable pageable);

    // 컨트롤러에서 바로 DTO로 받게 하려면 default 매핑 제공
    default Page<AdminBookingSummary> findAdminBookings(String status,
                                                        Long hotelId,
                                                        String hotelName,   // ★ 추가
                                                        String loginId,
                                                        LocalDate from,
                                                        LocalDate to,
                                                        Pageable pageable) {
        return findAdminRows(status, hotelId, hotelName, loginId, from, to, pageable)
                .map(r -> new AdminBookingSummary(
                        r.getBookingId(),
                        r.getStatus(),
                        r.getHotelName(),
                        r.getRoomTypeName(),
                        toLocal(r.getCheckIn()),
                        toLocal(r.getCheckOut()),
                        r.getNights(),
                        r.getGuests(),
                        r.getTotalAmount(),
                        r.getCurrency(),
                        r.getUserId(),
                        r.getUserLoginId(),
                        r.getUserName(),
                        r.getReceiptUrl()
                ));
    }

    private static LocalDate toLocal(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDate ld) return ld;
        if (v instanceof Date sd) return sd.toLocalDate();
        return LocalDate.parse(String.valueOf(v));
    }

    // 네이티브 결과 매핑용 프로젝션
    interface AdminBookingRow {
        Long getBookingId();
        String getStatus();
        String getHotelName();
        String getRoomTypeName();
        Date getCheckIn();
        Date getCheckOut();
        Integer getNights();
        Integer getGuests();
        Integer getTotalAmount();
        String getCurrency();
        Long getUserId();
        String getUserLoginId();
        String getUserName();
        String getReceiptUrl();
    }
}
