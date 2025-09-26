// src/main/java/com/example/hotelres/settlement/repo/SettlementCalcRepository.java
package com.example.hotelres.settlement.repo;

import com.example.hotelres.settlement.dto.HotelSummaryDTO;
import com.example.hotelres.settlement.dto.SettlementLineDTO;
import com.example.hotelres.settlement.entity.SettlementItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SettlementCalcRepository extends JpaRepository<SettlementItem, Long> {

  /**
   * 라인 상세 (주간 · 호텔 × 객실타입 단일 합계)
   * - 쿠폰은 booking 단위 합계를 아이템 수로 균등 배분
   * - 모든 합계식에 COALESCE 적용
   * - checkoutDate 제거 → 주간 합산으로만 표기
   */
  @Query(value = """
    WITH
      bk_discount AS (
        SELECT ci.used_booking_id AS booking_id, SUM(c.amount) AS discount_amount
        FROM coupon_issuance ci
        JOIN coupons c ON c.id = ci.coupon_id
        WHERE ci.status = 'USED'
        GROUP BY ci.used_booking_id
      ),
      bk_item_cnt AS (
        SELECT bi.booking_id, COUNT(*) AS item_cnt
        FROM booking_items bi
        GROUP BY bi.booking_id
      ),
      base AS (
        SELECT
          b.id            AS booking_id,
          b.hotel_id      AS hotelId,
          h.name          AS hotelName,
          bi.room_type_id AS roomTypeId,
          rt.name         AS roomTypeName,
          bi.price_total  AS item_gross,
          COALESCE( ROUND(COALESCE(d.discount_amount,0) / NULLIF(ic.item_cnt,0)), 0 ) AS discount_per_item
        FROM booking_items bi
        JOIN bookings b         ON b.id = bi.booking_id
        JOIN hotels   h         ON h.id = b.hotel_id
        LEFT JOIN room_types rt ON rt.id = bi.room_type_id
        LEFT JOIN bk_discount d ON d.booking_id = b.id
        LEFT JOIN bk_item_cnt ic ON ic.booking_id = b.id
        WHERE b.status='CONFIRMED'
          AND b.currency='KRW'
          AND b.check_out BETWEEN :start AND :end
          AND (:hotelId    IS NULL OR :hotelId    = 0 OR b.hotel_id     = :hotelId)
          AND (:roomTypeId IS NULL OR :roomTypeId = 0 OR bi.room_type_id = :roomTypeId)
      )
    SELECT
      hotelId,
      hotelName,
      roomTypeId,
      roomTypeName,
      COUNT(DISTINCT booking_id)                                            AS bookingCount,
      COALESCE(SUM(item_gross), 0)                                          AS grossAmount,
      COALESCE(SUM(discount_per_item), 0)                                   AS discountAmount,
      COALESCE(SUM(ROUND((item_gross - discount_per_item) * 0.15)), 0)      AS platformFeeAmount,
      COALESCE(SUM((item_gross - discount_per_item)
          - ROUND((item_gross - discount_per_item) * 0.15)), 0)             AS netToHotel
    FROM base
    GROUP BY hotelId, hotelName, roomTypeId, roomTypeName
    ORDER BY hotelName, roomTypeName
  """, nativeQuery = true)
  List<Object[]> calculateLinesRaw(@Param("hotelId") Long hotelId,
                                   @Param("roomTypeId") Long roomTypeId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

  default List<SettlementLineDTO> calculateLines(Long hotelId, Long roomTypeId, LocalDate start, LocalDate end) {
    return calculateLinesRaw(hotelId, roomTypeId, start, end).stream().map(r ->
      new SettlementLineDTO(
        ((Number) r[0]).longValue(),  // hotelId
        (String) r[1],                // hotelName
        ((Number) r[2]).longValue(),  // roomTypeId
        (String) r[3],                // roomTypeName
        ((Number) r[4]).longValue(),  // bookingCount
        ((Number) r[5]).longValue(),  // gross
        ((Number) r[6]).longValue(),  // discount
        ((Number) r[7]).longValue(),  // fee
        ((Number) r[8]).longValue()   // net
      )
    ).toList();
  }

  /**
   * 호텔 요약 (주간 · 호텔 단위 합계)
   */
  @Query(value = """
    WITH
      bk_discount AS (
        SELECT ci.used_booking_id AS booking_id, SUM(c.amount) AS discount_amount
        FROM coupon_issuance ci
        JOIN coupons c ON c.id = ci.coupon_id
        WHERE ci.status = 'USED'
        GROUP BY ci.used_booking_id
      ),
      bk_item_cnt AS (
        SELECT bi.booking_id, COUNT(*) AS item_cnt
        FROM booking_items bi
        GROUP BY bi.booking_id
      ),
      base AS (
        SELECT
          b.id           AS booking_id,
          b.hotel_id     AS hotelId,
          h.name         AS hotelName,
          bi.price_total AS item_gross,
          COALESCE( ROUND(COALESCE(d.discount_amount,0) / NULLIF(ic.item_cnt,0)), 0 ) AS discount_per_item
        FROM booking_items bi
        JOIN bookings b   ON b.id = bi.booking_id
        JOIN hotels   h   ON h.id = b.hotel_id
        LEFT JOIN bk_discount d  ON d.booking_id = b.id
        LEFT JOIN bk_item_cnt ic ON ic.booking_id = b.id
        WHERE b.status='CONFIRMED'
          AND b.currency='KRW'
          AND b.check_out BETWEEN :start AND :end
          AND (:hotelId    IS NULL OR :hotelId    = 0 OR b.hotel_id     = :hotelId)
          AND (:roomTypeId IS NULL OR :roomTypeId = 0 OR bi.room_type_id = :roomTypeId)
      )
    SELECT
      hotelId,
      hotelName,
      COUNT(DISTINCT booking_id)                                           AS bookingCount,
      COALESCE(SUM(item_gross), 0)                                         AS grossSum,
      COALESCE(SUM(discount_per_item), 0)                                  AS discountSum,
      COALESCE(SUM(ROUND((item_gross - discount_per_item) * 0.15)), 0)     AS feeSum,
      COALESCE(SUM((item_gross - discount_per_item)
          - ROUND((item_gross - discount_per_item) * 0.15)), 0)            AS netSum
    FROM base
    GROUP BY hotelId, hotelName
    ORDER BY hotelName
  """, nativeQuery = true)
  List<Object[]> summarizeByHotelRaw(@Param("hotelId") Long hotelId,
                                     @Param("roomTypeId") Long roomTypeId,
                                     @Param("start") LocalDate start,
                                     @Param("end") LocalDate end);

  default List<HotelSummaryDTO> summarizeByHotel(Long hotelId, Long roomTypeId, LocalDate start, LocalDate end) {
    return summarizeByHotelRaw(hotelId, roomTypeId, start, end).stream().map(r ->
      new HotelSummaryDTO(
        ((Number) r[0]).longValue(),   // hotelId
        (String) r[1],                 // hotelName
        ((Number) r[2]).longValue(),   // bookingCount
        ((Number) r[3]).longValue(),   // grossSum
        ((Number) r[4]).longValue(),   // discountSum
        ((Number) r[5]).longValue(),   // feeSum
        ((Number) r[6]).longValue()    // netSum
      )
    ).toList();
  }
}
