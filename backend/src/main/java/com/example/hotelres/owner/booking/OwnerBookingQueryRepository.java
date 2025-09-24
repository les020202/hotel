// src/main/java/com/example/hotelres/owner/booking/OwnerBookingQueryRepository.java
package com.example.hotelres.owner.booking;

import com.example.hotelres.owner.BookingEntity;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.util.List;

public interface OwnerBookingQueryRepository extends Repository<BookingEntity, Long> {

    @Query(value = """
        SELECT
          b.id                    AS bookingId,
          CAST(b.status AS CHAR)  AS status,
          u.login_id              AS userLoginId,
          u.name                  AS userName,
          rt.name                 AS roomTypeName,
          DATE(b.check_in)        AS checkIn,
          b.nights                AS nights,
          b.guests                AS guests,
          b.total_amount          AS totalAmount,
          b.currency              AS currency
        FROM bookings b
          JOIN users u           ON u.id = b.user_id
          JOIN booking_items bi  ON bi.booking_id = b.id
          JOIN room_types rt     ON rt.id = bi.room_type_id
          /* ★ 오너 스코프: 해당 호텔 + 로그인ID 매핑 */
          JOIN hotel_owners ho   ON ho.hotel_id = b.hotel_id
                                AND ho.user_login_id = :ownerLoginId
        WHERE b.hotel_id = :hotelId
        ORDER BY b.id DESC
        """, nativeQuery = true)
    List<Row> findRows(@Param("hotelId") Long hotelId,
                       @Param("ownerLoginId") String ownerLoginId);

    default List<OwnerBookingSummary> listForOwner(Long hotelId, String ownerLoginId) {
        return findRows(hotelId, ownerLoginId).stream()
                .map(r -> new OwnerBookingSummary(
                        r.getBookingId(),
                        r.getStatus(),
                        r.getUserLoginId(),
                        r.getUserName(),
                        r.getRoomTypeName(),
                        toLocal(r.getCheckIn()),
                        r.getNights(),
                        r.getGuests(),
                        r.getTotalAmount(),
                        r.getCurrency()
                ))
                .toList();
    }

    private static java.time.LocalDate toLocal(Object v) {
        if (v == null) return null;
        if (v instanceof java.time.LocalDate ld) return ld;
        if (v instanceof Date sd) return sd.toLocalDate();
        return java.time.LocalDate.parse(String.valueOf(v));
    }

    interface Row {
        Long getBookingId();
        String getStatus();
        String getUserLoginId();
        String getUserName();
        String getRoomTypeName();
        Date getCheckIn();
        Integer getNights();
        Integer getGuests();
        Integer getTotalAmount();
        String getCurrency();
    }
}
