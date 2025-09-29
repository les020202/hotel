// src/main/java/com/example/hotelres/owner/sales/OwnerSalesRepository.java
package com.example.hotelres.owner.sales;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class OwnerSalesRepository {

  @PersistenceContext
  private EntityManager em;

  /** 이번 주(월~일) 7행: payments.approved_at 기준, SUCCEEDED + KRW */
  public List<Object[]> currentWeekByDay(Long hotelId) {
    return em.createNativeQuery("""
        WITH RECURSIVE
        week_start AS ( SELECT (CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY) AS d0 ),
        days AS (
          SELECT d0 AS d FROM week_start
          UNION ALL
          SELECT d + INTERVAL 1 DAY FROM days, week_start
          WHERE d < week_start.d0 + INTERVAL 6 DAY
        ),
        sales AS (
          SELECT DATE(p.approved_at) AS d, SUM(p.amount) AS amt
          FROM payments p
          JOIN bookings b ON b.id = p.booking_id
          JOIN week_start ws
          WHERE b.hotel_id = :hid
            AND p.status   = 'SUCCEEDED'
            AND p.currency = 'KRW'
            AND p.approved_at >= ws.d0
            AND p.approved_at <  ws.d0 + INTERVAL 7 DAY
          GROUP BY DATE(p.approved_at)
        )
        SELECT days.d AS day, COALESCE(s.amt,0) AS amount
        FROM days
        LEFT JOIN sales s ON s.d = days.d
        ORDER BY days.d
        """)
      .setParameter("hid", hotelId)
      .getResultList();
  }

  /** 특정 주(월요일 기준) 7행 */
  public List<Object[]> weekByDay(Long hotelId, LocalDate weekMonday) {
    return em.createNativeQuery("""
        WITH RECURSIVE
        days AS (
          SELECT DATE(:weekMon) AS d
          UNION ALL
          SELECT d + INTERVAL 1 DAY FROM days
          WHERE d < DATE(:weekMon) + INTERVAL 6 DAY
        ),
        sales AS (
          SELECT DATE(p.approved_at) AS d, SUM(p.amount) AS amt
          FROM payments p
          JOIN bookings b ON b.id = p.booking_id
          WHERE b.hotel_id = :hid
            AND p.status   = 'SUCCEEDED'
            AND p.currency = 'KRW'
            AND p.approved_at >= DATE(:weekMon)
            AND p.approved_at <  DATE(:weekMon) + INTERVAL 7 DAY
          GROUP BY DATE(p.approved_at)
        )
        SELECT days.d AS day, COALESCE(s.amt,0) AS amount
        FROM days
        LEFT JOIN sales s ON s.d = days.d
        ORDER BY days.d
        """)
      .setParameter("hid", hotelId)
      .setParameter("weekMon", weekMonday)
      .getResultList();
  }

  /** 월별 시계열 (프론트에서 월 탭 사용하는 경우) */
  public List<Object[]> monthSeries(Long hotelId,
                                    LocalDate startMonthInclusive,
                                    LocalDate endMonthExclusive) {
    LocalDateTime startDt = startMonthInclusive.withDayOfMonth(1).atStartOfDay();
    LocalDateTime endDt   = endMonthExclusive.withDayOfMonth(1).atStartOfDay();
    return em.createNativeQuery("""
        SELECT DATE_FORMAT(p.approved_at, '%Y-%m') AS label,
               COALESCE(SUM(p.amount),0)          AS amount
        FROM payments p
        JOIN bookings b ON b.id = p.booking_id
        WHERE b.hotel_id = :hid
          AND p.status   = 'SUCCEEDED'
          AND p.currency = 'KRW'
          AND p.approved_at >= :startDt
          AND p.approved_at <  :endDt
        GROUP BY DATE_FORMAT(p.approved_at, '%Y-%m')
        ORDER BY label
        """)
      .setParameter("hid", hotelId)
      .setParameter("startDt", startDt)
      .setParameter("endDt", endDt)
      .getResultList();
  }

  /** 추가: 임의 구간(start~end '포함')을 일별로 그대로 반환 */
  public List<Object[]> dailyByRange(Long hotelId, LocalDate startInclusive, LocalDate endInclusive) {
    return em.createNativeQuery("""
        WITH RECURSIVE
        days AS (
          SELECT DATE(:startD) AS d
          UNION ALL
          SELECT d + INTERVAL 1 DAY FROM days
          WHERE d < DATE(:endD)
        ),
        sales AS (
          SELECT DATE(p.approved_at) AS d, SUM(p.amount) AS amt
          FROM payments p
          JOIN bookings b ON b.id = p.booking_id
          WHERE b.hotel_id = :hid
            AND p.status   = 'SUCCEEDED'
            AND p.currency = 'KRW'
            AND p.approved_at >= DATE(:startD)
            AND p.approved_at <  DATE(:endD) + INTERVAL 1 DAY  -- end 포함
          GROUP BY DATE(p.approved_at)
        )
        SELECT days.d AS day, COALESCE(s.amt,0) AS amount
        FROM days
        LEFT JOIN sales s ON s.d = days.d
        ORDER BY days.d
        """)
      .setParameter("hid", hotelId)
      .setParameter("startD", startInclusive)
      .setParameter("endD", endInclusive)
      .getResultList();
  }
}
