// src/main/java/com/example/hotelres/admin/AdminDashboardController.java
package com.example.hotelres.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    @PersistenceContext
    private EntityManager em;

    /* === DTOs === */
    public record Overview(long totalUsers, long totalHotels, long todayBookings, long pendingHotelApps) {}
    public record DailyRevenuePoint(LocalDate date, BigDecimal amount) {}
    public record DailyRevenue(List<DailyRevenuePoint> points) {}
    public record TopHotel(Long hotelId, String hotelName, long bookingCount, BigDecimal revenue) {}
    public record TopHotels(List<TopHotel> items) {}

    /* 1) 개요 */
    @GetMapping("/overview")
    public Overview overview() {
        long users  = safeCount("select count(*) from users");
        long hotels = safeCount("select count(*) from hotels");

        long todayA = safeCount("select count(*) from bookings where DATE(created_at) = CURRENT_DATE");
        long todayB = safeCount("select count(*) from bookings where DATE(booked_at)  = CURRENT_DATE");
        long todayBookings = todayA + todayB;

        long pendingApps = safeCount("select count(*) from hotel_applications where status = 'PENDING'");

        return new Overview(users, hotels, todayBookings, pendingApps);
    }

    /* 2) 최근 N일 매출 */
    @GetMapping("/revenue/daily")
    public DailyRevenue daily(@RequestParam(defaultValue = "7") int days) {
        int d = Math.max(1, Math.min(days, 60));
        List<DailyRevenuePoint> list = new ArrayList<>();
        try {
            String sql =
                "select DATE(created_at) d, coalesce(sum(amount),0) amt " +
                "from payments " +
                "where status in ('SUCCESS','PAID') " +
                "  and created_at >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) " +
                "group by DATE(created_at) " +
                "order by d";

            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(sql)
                    .setParameter(1, d)
                    .getResultList();

            for (Object[] r : rows) {
                java.sql.Date dt = (java.sql.Date) r[0];
                BigDecimal amt   = r[1] != null ? (BigDecimal) r[1] : BigDecimal.ZERO;
                list.add(new DailyRevenuePoint(dt.toLocalDate(), amt));
            }
        } catch (Exception ignore) { /* 테이블/컬럼 없어도 빈 배열 */ }
        return new DailyRevenue(list);
    }

    /* 3) 기간별 TOP 호텔 */
    @GetMapping("/hotels/top")
    public TopHotels top(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<TopHotel> items = new ArrayList<>();
        try {
            String sql =
                "select b.hotel_id as hid, h.name as hname, count(*) as bc, " +
                "       coalesce(sum(b.total_price),0) as rev " +
                "from bookings b " +
                "join hotels h on h.id = b.hotel_id " +
                "where b.check_in >= ? and b.check_out <= ? " +
                "group by b.hotel_id, h.name " +
                "order by rev desc " +
                "limit 10";

            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(sql)
                    .setParameter(1, java.sql.Date.valueOf(from))
                    .setParameter(2, java.sql.Date.valueOf(to))
                    .getResultList();

            for (Object[] r : rows) {
                Long hid        = ((Number) r[0]).longValue();
                String hname    = (String) r[1];
                long cnt        = ((Number) r[2]).longValue();
                BigDecimal rev  = r[3] != null ? (BigDecimal) r[3] : BigDecimal.ZERO;
                items.add(new TopHotel(hid, hname, cnt, rev));
            }
        } catch (Exception ignore) { /* 스키마 다르면 빈 배열 */ }
        return new TopHotels(items);
    }

    /* 공통: 존재 안해도 0으로 */
    private long safeCount(String sql) {
        try {
            Number n = (Number) em.createNativeQuery(sql).getSingleResult();
            return n != null ? n.longValue() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}
