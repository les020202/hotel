package com.example.hotelres.admin;

import com.example.hotelres.admin.dto.DecompRow;
import com.example.hotelres.admin.dto.RankingRow;
import com.example.hotelres.admin.dto.RefundRateRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class AdminDashboardRepository {

    @PersistenceContext
    EntityManager em;

    // 공통 상수
    private static final String SUCCEEDED = "SUCCEEDED";

    // =========================
    // Regions
    // =========================
    public List<String> listRegions() {
        @SuppressWarnings("unchecked")
        List<String> rows = em.createNativeQuery("""
            SELECT DISTINCT region
              FROM hotels
             WHERE region IS NOT NULL AND region <> ''
             ORDER BY region
        """).getResultList();
        return rows;
    }

    // =========================
    // Top cards (GMV / Refunds)
    // =========================
    public long sumGmvByDate(String region, String hotel, LocalDate from, LocalDate to){
        String sql = """
           SELECT COALESCE(SUM(p.amount),0)
             FROM payments p
             JOIN bookings b ON p.booking_id = b.id
             JOIN hotels   h ON b.hotel_id   = h.id
            WHERE p.status = :succ
              AND p.approved_at >= :fromTs
              AND p.approved_at <  :toTs
              AND (:region IS NULL OR h.region = :region)
              AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                    OR CAST(h.id AS CHAR) = :hotel))
        """;

        Object result = em.createNativeQuery(sql)
                .setParameter("succ", SUCCEEDED)
                .setParameter("fromTs", java.sql.Timestamp.valueOf(from.atStartOfDay()))
                .setParameter("toTs",   java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()))
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getSingleResult();

        return ((Number) result).longValue();
    }

    public long sumRefundByDate(String region, String hotel, LocalDate from, LocalDate to){
        String sql = """
           SELECT COALESCE(SUM(b.total_amount),0)
             FROM bookings b
             JOIN hotels h ON b.hotel_id = h.id
            WHERE b.status = 'CANCELLED'
              AND b.updated_at >= :fromTs
              AND b.updated_at <  :toTs
              AND (:region IS NULL OR h.region = :region)
              AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                    OR CAST(h.id AS CHAR) = :hotel))
        """;

        Object result = em.createNativeQuery(sql)
                .setParameter("fromTs", java.sql.Timestamp.valueOf(from.atStartOfDay()))
                .setParameter("toTs",   java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()))
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getSingleResult();

        return ((Number) result).longValue();
    }

    // =========================
    // KPI (ADR / Occupancy / RevPAR / LongStay)
    // =========================
    public double calcAdr(String region, String hotel, LocalDate from, LocalDate to) {
        String sql = """
           SELECT COALESCE(SUM(b.total_amount) / NULLIF(SUM(b.nights),0), 0)
             FROM bookings b
             JOIN hotels h ON b.hotel_id = h.id
            WHERE b.status = 'CONFIRMED'
              AND b.check_in BETWEEN :from AND :to
              AND (:region IS NULL OR h.region = :region)
              AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                    OR CAST(h.id AS CHAR) = :hotel))
        """;

        Object result = em.createNativeQuery(sql)
                .setParameter("from", from)
                .setParameter("to",   to)
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getSingleResult();

        return toDouble(result);
    }

    public double calcOccupancy(String region, String hotel, LocalDate from, LocalDate to) {
        String sql = """
           SELECT COALESCE(SUM(bd.booked) / NULLIF(SUM(bd.allotment),0), 0)
             FROM booking_day bd
             JOIN hotels h ON bd.hotel_id = h.id
            WHERE bd.stay_date BETWEEN :from AND :to
              AND (:region IS NULL OR h.region = :region)
              AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                    OR CAST(h.id AS CHAR) = :hotel))
        """;

        Object result = em.createNativeQuery(sql)
                .setParameter("from", from)
                .setParameter("to",   to)
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getSingleResult();

        return toDouble(result);
    }

    public double calcRevpar(String region, String hotel, LocalDate from, LocalDate to) {
        String sql = """
           SELECT COALESCE(SUM(b.total_amount) / NULLIF(SUM(bd.allotment),0), 0)
             FROM bookings b
             JOIN hotels h    ON b.hotel_id = h.id
             JOIN booking_day bd ON bd.hotel_id = h.id
            WHERE b.status = 'CONFIRMED'
              AND b.check_in BETWEEN :from AND :to
              AND bd.stay_date BETWEEN :from AND :to
              AND (:region IS NULL OR h.region = :region)
              AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                    OR CAST(h.id AS CHAR) = :hotel))
        """;

        Object result = em.createNativeQuery(sql)
                .setParameter("from", from)
                .setParameter("to",   to)
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getSingleResult();

        return toDouble(result);
    }

    public double calcLongstay(String region, String hotel, LocalDate from, LocalDate to) {
        String sql = """
           SELECT COALESCE(SUM(CASE WHEN b.nights >= 3 THEN 1 ELSE 0 END) / NULLIF(COUNT(*),0), 0)
             FROM bookings b
             JOIN hotels h ON b.hotel_id = h.id
            WHERE b.status = 'CONFIRMED'
              AND b.check_in BETWEEN :from AND :to
              AND (:region IS NULL OR h.region = :region)
              AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                    OR CAST(h.id AS CHAR) = :hotel))
        """;

        Object result = em.createNativeQuery(sql)
                .setParameter("from", from)
                .setParameter("to",   to)
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getSingleResult();

        return toDouble(result);
    }

    // =========================
    // Buckets
    // =========================
    public List<AdminDashboardService.BucketRow> dailyBuckets(String region, String hotel, LocalDate from, LocalDate to){
        String sql = """
            SELECT DATE_FORMAT(p.approved_at, '%Y-%m-%d') AS day_bucket,
                   COALESCE(SUM(p.amount),0)              AS total_amount,
                   COUNT(DISTINCT b.id)                   AS booking_count
              FROM payments p
              JOIN bookings b ON p.booking_id = b.id
              JOIN hotels   h ON b.hotel_id   = h.id
             WHERE p.status = :succ
               AND p.approved_at >= :fromTs
               AND p.approved_at <  :toTs
               AND (:region IS NULL OR h.region = :region)
               AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                     OR CAST(h.id AS CHAR) = :hotel))
             GROUP BY DATE_FORMAT(p.approved_at, '%Y-%m-%d')
             ORDER BY day_bucket
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("succ", SUCCEEDED)
                .setParameter("fromTs", java.sql.Timestamp.valueOf(from.atStartOfDay()))
                .setParameter("toTs",   java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()))
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getResultList();

        return rows.stream()
                .map(r -> new AdminDashboardService.BucketRow(
                        (String) r[0],
                        ((Number) r[1]).longValue(),
                        ((Number) r[2]).intValue()
                ))
                .toList();
    }

    public List<AdminDashboardService.BucketRow> hourlyBuckets(String region, String hotel, LocalDate target) {
        String sql = """
            SELECT DATE_FORMAT(p.approved_at, '%H:00') AS hour_bucket,
                   COALESCE(SUM(p.amount),0)          AS total_amount,
                   COUNT(DISTINCT b.id)               AS booking_count
              FROM payments p
              JOIN bookings b ON p.booking_id = b.id
              JOIN hotels   h ON b.hotel_id   = h.id
             WHERE p.status = :succ
               AND DATE(p.approved_at) = :target
               AND (:region IS NULL OR h.region = :region)
               AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                     OR CAST(h.id AS CHAR) = :hotel))
             GROUP BY DATE_FORMAT(p.approved_at, '%H:00')
             ORDER BY hour_bucket
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("succ", SUCCEEDED)
                .setParameter("target", target)
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getResultList();

        return rows.stream()
                .map(r -> new AdminDashboardService.BucketRow(
                        (String) r[0],
                        ((Number) r[1]).longValue(),
                        ((Number) r[2]).intValue()
                ))
                .toList();
    }

    // =========================
    // Ranks
    // =========================
    @SuppressWarnings("unchecked")
    public List<RankingRow> revenueRank(String range, String region, String hotel, LocalDate from, LocalDate to) {
        LocalDate prevFrom = from.minusDays(daysBetween(from, to));
        LocalDate prevTo   = from.minusDays(1);

        String curSql = """
           SELECT h.id, h.name, h.region, COUNT(DISTINCT b.id) AS bookings, COALESCE(SUM(p.amount),0) AS revenue
             FROM payments p
             JOIN bookings b ON p.booking_id = b.id
             JOIN hotels   h ON b.hotel_id   = h.id
            WHERE p.status = :succ
              AND p.approved_at >= :fromTs
              AND p.approved_at <  :toTs
              AND (:region IS NULL OR h.region = :region)
              AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                     OR CAST(h.id AS CHAR) = :hotel))
            GROUP BY h.id, h.name, h.region
        """;

        String prevSql = """
           SELECT h.id, COALESCE(SUM(p.amount),0) AS revenue
             FROM payments p
             JOIN bookings b ON p.booking_id = b.id
             JOIN hotels   h ON b.hotel_id   = h.id
            WHERE p.status = :succ
              AND p.approved_at >= :fromTs
              AND p.approved_at <  :toTs
              AND (:region IS NULL OR h.region = :region)
              AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                     OR CAST(h.id AS CHAR) = :hotel))
            GROUP BY h.id
        """;

        List<Object[]> curRows = em.createNativeQuery(curSql)
                .setParameter("succ", SUCCEEDED)
                .setParameter("fromTs", java.sql.Timestamp.valueOf(from.atStartOfDay()))
                .setParameter("toTs",   java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()))
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getResultList();

        List<Object[]> prevRows = em.createNativeQuery(prevSql)
                .setParameter("succ", SUCCEEDED)
                .setParameter("fromTs", java.sql.Timestamp.valueOf(prevFrom.atStartOfDay()))
                .setParameter("toTs",   java.sql.Timestamp.valueOf(prevTo.plusDays(1).atStartOfDay()))
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getResultList();

        var prevMap = prevRows.stream().collect(Collectors.toMap(
                r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).longValue()
        ));

        return curRows.stream().map(r -> {
            Long hid   = ((Number) r[0]).longValue();
            String hname = (String) r[1];
            String rgn   = (String) r[2];
            Integer cnt  = ((Number) r[3]).intValue();
            Long rev     = ((Number) r[4]).longValue();
            Long prevRev = prevMap.getOrDefault(hid, 0L);
            double delta = prevRev == 0 ? (rev > 0 ? 1.0 : 0.0) : (rev - prevRev) / (double) prevRev;
            return RankingRow.builder()
                    .hotelId(hid).hotelName(hname).region(rgn).bookings(cnt).revenue(rev).delta(delta).build();
        }).toList();
    }

    public List<RefundRateRow> refundRateRank(String range, int minVolume, String region, String hotel, LocalDate from, LocalDate to){
        String sql = """
          SELECT h.id, h.name, h.region,
                 COUNT(DISTINCT b.id) AS bookings,
                 SUM(CASE WHEN r.id IS NOT NULL THEN 1 ELSE 0 END) AS refunds
            FROM bookings b
            JOIN hotels h        ON h.id = b.hotel_id
       LEFT JOIN payments p      ON p.booking_id = b.id AND p.status = :succ
       LEFT JOIN refunds r       ON r.payment_id = p.id AND r.status = :succ
           WHERE b.check_out BETWEEN :from AND :to
             AND (:region IS NULL OR h.region = :region)
             AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                   OR CAST(h.id AS CHAR) = :hotel))
           GROUP BY h.id, h.name, h.region
           HAVING COUNT(DISTINCT b.id) >= :minVolume
           ORDER BY (SUM(CASE WHEN r.id IS NOT NULL THEN 1 ELSE 0 END) * 1.0 / COUNT(DISTINCT b.id)) ASC
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("succ", SUCCEEDED)
                .setParameter("from", from)
                .setParameter("to",   to)
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .setParameter("minVolume", minVolume)
                .getResultList();

        return rows.stream().map(r -> {
            int bookings = ((Number) r[3]).intValue();
            int refunds  = ((Number) r[4]).intValue();
            double rate  = bookings==0 ? 0.0 : refunds/(double)bookings;
            return RefundRateRow.builder()
                    .hotelId(((Number) r[0]).longValue())
                    .hotelName((String) r[1])
                    .region((String) r[2])
                    .bookings(bookings)
                    .refunds(refunds)
                    .refundRate(rate)
                    .build();
        }).toList();
    }

    @SuppressWarnings("unchecked")
    public List<RankingRow> velocityRank(String range, int limit, String region, String hotel, LocalDate from, LocalDate to) {
        LocalDate curFrom = to.minusDays(6);
        LocalDate prevFrom = curFrom.minusDays(7);
        LocalDate prevTo   = curFrom.minusDays(1);

        String curSql = """
            SELECT h.id, h.name, h.region,
                   COUNT(DISTINCT b.id)            AS bookings,
                   COALESCE(SUM(p.amount), 0)      AS revenue
              FROM payments p
              JOIN bookings b ON p.booking_id = b.id
              JOIN hotels   h ON b.hotel_id   = h.id
             WHERE p.status = :succ
               AND p.approved_at >= :fromTs
               AND p.approved_at <  :toTs
               AND (:region IS NULL OR h.region = :region)
               AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                     OR CAST(h.id AS CHAR) = :hotel))
             GROUP BY h.id, h.name, h.region
        """;

        String prevSql = """
            SELECT h.id, COALESCE(SUM(p.amount),0) AS revenue
              FROM payments p
              JOIN bookings b ON p.booking_id = b.id
              JOIN hotels   h ON b.hotel_id   = h.id
             WHERE p.status = :succ
               AND p.approved_at >= :fromTs
               AND p.approved_at <  :toTs
               AND (:region IS NULL OR h.region = :region)
               AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                     OR CAST(h.id AS CHAR) = :hotel))
             GROUP BY h.id
        """;

        var curRows = em.createNativeQuery(curSql)
            .setParameter("succ", SUCCEEDED)
            .setParameter("fromTs", java.sql.Timestamp.valueOf(curFrom.atStartOfDay()))
            .setParameter("toTs",   java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()))
            .setParameter("region", emptyToNull(region))
            .setParameter("hotel",  emptyToNull(hotel))
            .getResultList();

        var prevRows = em.createNativeQuery(prevSql)
            .setParameter("succ", SUCCEEDED)
            .setParameter("fromTs", java.sql.Timestamp.valueOf(prevFrom.atStartOfDay()))
            .setParameter("toTs",   java.sql.Timestamp.valueOf(prevTo.plusDays(1).atStartOfDay()))
            .setParameter("region", emptyToNull(region))
            .setParameter("hotel",  emptyToNull(hotel))
            .getResultList();

        var prevMap = ((List<Object[]>) prevRows).stream().collect(
            java.util.stream.Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> ((Number) r[1]).longValue()
            )
        );

        return ((List<Object[]>) curRows).stream().map(r -> {
            Long   hid  = ((Number) r[0]).longValue();
            String nm   = (String) r[1];
            String rg   = (String) r[2];
            Integer cnt = ((Number) r[3]).intValue();  // ✅ 예약수 채움
            long   rev  = ((Number) r[4]).longValue();
            long   prv  = prevMap.getOrDefault(hid, 0L);
            double d    = prv == 0 ? (rev > 0 ? 1.0 : 0.0) : (rev - prv) / (double) prv;

            return RankingRow.builder()
                .hotelId(hid).hotelName(nm).region(rg)
                .bookings(cnt)                // ✅ 더 이상 null 아님
                .revenue(rev).delta(d)
                .build();
        }).sorted((a,b) -> Double.compare(Math.abs(b.getDelta()), Math.abs(a.getDelta())))
          .limit(limit).toList();
    }


    // =========================
    // Decomposition
    // =========================
    @SuppressWarnings("unchecked")
    public List<DecompRow> decompByRegion(String region, String hotel, LocalDate from, LocalDate to) {
        String sql = """
            SELECT h.region AS region_key,
                   h.region AS region_name,
                   COALESCE(SUM(p.amount),0) AS revenue
              FROM payments p
              JOIN bookings b ON p.booking_id = b.id
              JOIN hotels   h ON b.hotel_id   = h.id
             WHERE p.status = :succ
               AND p.approved_at >= :fromTs
               AND p.approved_at <  :toTs
               AND (:region IS NULL OR h.region = :region)
               AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                     OR CAST(h.id AS CHAR) = :hotel))
             GROUP BY h.region
             ORDER BY revenue DESC
        """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("succ", SUCCEEDED)
                .setParameter("fromTs", java.sql.Timestamp.valueOf(from.atStartOfDay()))
                .setParameter("toTs",   java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()))
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getResultList();

        return rows.stream()
                .map(r -> new DecompRow((String) r[0], (String) r[1], ((Number) r[2]).longValue(), 0.0))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<DecompRow> decompByHotel(String region, String hotel, LocalDate from, LocalDate to) {
        String sql = """
            SELECT CAST(h.id AS CHAR) AS hotel_key,
                   h.name            AS hotel_name,
                   COALESCE(SUM(p.amount),0) AS revenue
              FROM payments p
              JOIN bookings b ON p.booking_id = b.id
              JOIN hotels   h ON b.hotel_id   = h.id
             WHERE p.status = :succ
               AND p.approved_at >= :fromTs
               AND p.approved_at <  :toTs
               AND (:region IS NULL OR h.region = :region)
               AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                     OR CAST(h.id AS CHAR) = :hotel))
             GROUP BY h.id, h.name
             ORDER BY revenue DESC
        """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("succ", SUCCEEDED)
                .setParameter("fromTs", java.sql.Timestamp.valueOf(from.atStartOfDay()))
                .setParameter("toTs",   java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()))
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getResultList();

        return rows.stream()
                .map(r -> new DecompRow((String) r[0], (String) r[1], ((Number) r[2]).longValue(), 0.0))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<DecompRow> decompByChannel(String region, String hotel, LocalDate from, LocalDate to) {
        String sql = """
            SELECT p.method AS channel_key,
                   p.method AS channel_name,
                   COALESCE(SUM(p.amount),0) AS revenue
              FROM payments p
              JOIN bookings b ON p.booking_id = b.id
              JOIN hotels   h ON b.hotel_id   = h.id
             WHERE p.status = :succ
               AND p.approved_at >= :fromTs
               AND p.approved_at <  :toTs
               AND (:region IS NULL OR h.region = :region)
               AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                     OR CAST(h.id AS CHAR) = :hotel))
             GROUP BY p.method
             ORDER BY revenue DESC
        """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("succ", SUCCEEDED)
                .setParameter("fromTs", java.sql.Timestamp.valueOf(from.atStartOfDay()))
                .setParameter("toTs",   java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()))
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getResultList();

        return rows.stream()
                .map(r -> new DecompRow((String) r[0], (String) r[1], ((Number) r[2]).longValue(), 0.0))
                .toList();
    }

    // =========================
    // Settlement Snapshot
    // =========================
    public AdminDashboardService.SettlementSnapshot settlementPending(String region, String hotel, LocalDate from, LocalDate to) {
        String curSql = """
            SELECT COALESCE(SUM(p.amount),0) AS pay_sum,
                   COALESCE(SUM(r.amount),0) AS refund_sum,
                   COUNT(DISTINCT p.id)      AS pay_cnt
              FROM payments p
              JOIN bookings b ON p.booking_id = b.id
              JOIN hotels   h ON b.hotel_id   = h.id
         LEFT JOIN refunds  r ON r.payment_id = p.id AND r.status = 'SUCCEEDED'
             WHERE p.status = 'SUCCEEDED'
               AND p.approved_at >= :fromTs
               AND p.approved_at <  :toTs
               AND (:region IS NULL OR h.region = :region)
               AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                     OR CAST(h.id AS CHAR) = :hotel))
        """;

        long days = daysBetween(from, to);
        LocalDate prevFrom = from.minusDays(days);
        LocalDate prevTo   = from.minusDays(1);

        List<Object[]> curRowList = em.createNativeQuery(curSql)
                .setParameter("fromTs", java.sql.Timestamp.valueOf(from.atStartOfDay()))
                .setParameter("toTs",   java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()))
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getResultList();

        List<Object[]> prevRowList = em.createNativeQuery(curSql)
                .setParameter("fromTs", java.sql.Timestamp.valueOf(prevFrom.atStartOfDay()))
                .setParameter("toTs",   java.sql.Timestamp.valueOf(prevTo.plusDays(1).atStartOfDay()))
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .getResultList();

        Object[] cur = (curRowList.isEmpty() ? new Object[]{0,0,0} : curRowList.get(0));
        Object[] prv = (prevRowList.isEmpty() ? new Object[]{0,0,0} : prevRowList.get(0));

        long curPaySum   = ((Number) (cur[0] == null ? 0 : cur[0])).longValue();
        long curRefSum   = ((Number) (cur[1] == null ? 0 : cur[1])).longValue();
        int  curPayCount = ((Number) (cur[2] == null ? 0 : cur[2])).intValue();

        long prevPaySum = ((Number) (prv[0] == null ? 0 : prv[0])).longValue();
        long prevRefSum = ((Number) (prv[1] == null ? 0 : prv[1])).longValue();

        long pending = Math.max(0, curPaySum - curRefSum);
        long prevPending = Math.max(0, prevPaySum - prevRefSum);
        double delta = (prevPending == 0)
                ? (pending > 0 ? 1.0 : 0.0)
                : (pending - prevPending) / (double) prevPending;

        String periodText = from.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"))
                          + "–"
                          + to.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"));

        return new AdminDashboardService.SettlementSnapshot(pending, delta, periodText, curPayCount);
    }

    // =========================
    // Top 10
    // =========================
    public List<RankingRow> top10TodayRevenue(String region, String hotel) {
        String curSql = """
           SELECT h.id, h.name, h.region,
                  COUNT(DISTINCT b.id) AS bookings,
                  COALESCE(SUM(p.amount),0) AS revenue
             FROM payments p
             JOIN bookings b ON p.booking_id = b.id
             JOIN hotels   h ON b.hotel_id   = h.id
            WHERE p.status = :succ
              AND DATE(p.approved_at) = CURDATE()
              AND (:region IS NULL OR h.region = :region)
              AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                    OR CAST(h.id AS CHAR) = :hotel))
            GROUP BY h.id, h.name, h.region
            ORDER BY revenue DESC
            LIMIT 5
        """;

        String prevSql = """
           SELECT h.id, COALESCE(SUM(p.amount),0) AS revenue
             FROM payments p
             JOIN bookings b ON p.booking_id = b.id
             JOIN hotels   h ON b.hotel_id   = h.id
            WHERE p.status = :succ
              AND DATE(p.approved_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)
              AND (:region IS NULL OR h.region = :region)
              AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                    OR CAST(h.id AS CHAR) = :hotel))
            GROUP BY h.id
        """;

        List<Object[]> curRows = em.createNativeQuery(curSql)
            .setParameter("succ", SUCCEEDED)
            .setParameter("region", emptyToNull(region))
            .setParameter("hotel",  emptyToNull(hotel))
            .getResultList();

        List<Object[]> prevRows = em.createNativeQuery(prevSql)
            .setParameter("succ", SUCCEEDED)
            .setParameter("region", emptyToNull(region))
            .setParameter("hotel",  emptyToNull(hotel))
            .getResultList();

        var prevMap = prevRows.stream().collect(
            java.util.stream.Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> ((Number) r[1]).longValue()
            )
        );

        return curRows.stream().map(r -> {
            long   hid  = ((Number) r[0]).longValue();
            String name = (String) r[1];
            String reg  = (String) r[2];
            int    cnt  = ((Number) r[3]).intValue();
            long   rev  = ((Number) r[4]).longValue();
            long   prv  = prevMap.getOrDefault(hid, 0L);
            double d    = (prv == 0) ? (rev > 0 ? 1.0 : 0.0) : (rev - prv) / (double) prv;

            return RankingRow.builder()
                .hotelId(hid).hotelName(name).region(reg)
                .bookings(cnt).revenue(rev).delta(d)
                .build();
        }).toList();
    }



    public List<RankingRow> top10MonthlyRevenue(String region, String hotel) {
        String curSql = """
           SELECT h.id, h.name, h.region,
                  COUNT(DISTINCT b.id) AS bookings,
                  COALESCE(SUM(p.amount),0) AS revenue
             FROM payments p
             JOIN bookings b ON p.booking_id = b.id
             JOIN hotels   h ON b.hotel_id   = h.id
            WHERE p.status = :succ
              AND p.approved_at >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
              AND p.approved_at <  DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH)
              AND (:region IS NULL OR h.region = :region)
              AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                    OR CAST(h.id AS CHAR) = :hotel))
            GROUP BY h.id, h.name, h.region
            ORDER BY revenue DESC
            LIMIT 5
        """;	

        String prevSql = """
           SELECT h.id, COALESCE(SUM(p.amount),0) AS revenue
             FROM payments p
             JOIN bookings b ON p.booking_id = b.id
             JOIN hotels   h ON b.hotel_id   = h.id
            WHERE p.status = :succ
              AND p.approved_at >= DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH)
              AND p.approved_at <  DATE_FORMAT(CURDATE(), '%Y-%m-01')
              AND (:region IS NULL OR h.region = :region)
              AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                    OR CAST(h.id AS CHAR) = :hotel))
            GROUP BY h.id
        """;

        var curRows = em.createNativeQuery(curSql)
            .setParameter("succ", SUCCEEDED)
            .setParameter("region", emptyToNull(region))
            .setParameter("hotel",  emptyToNull(hotel))
            .getResultList();

        var prevRows = em.createNativeQuery(prevSql)
            .setParameter("succ", SUCCEEDED)
            .setParameter("region", emptyToNull(region))
            .setParameter("hotel",  emptyToNull(hotel))
            .getResultList();

        var prevMap = ((List<Object[]>) prevRows).stream().collect(
            java.util.stream.Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> ((Number) r[1]).longValue()
            )
        );

        return ((List<Object[]>) curRows).stream().map(r -> {
            long   hid  = ((Number) r[0]).longValue();
            String name = (String) r[1];
            String reg  = (String) r[2];
            int    cnt  = ((Number) r[3]).intValue();
            long   rev  = ((Number) r[4]).longValue();
            long   prv  = prevMap.getOrDefault(hid, 0L);
            double d    = (prv == 0) ? (rev > 0 ? 1.0 : 0.0) : (rev - prv) / (double) prv;

            return RankingRow.builder()
                .hotelId(hid).hotelName(name).region(reg)
                .bookings(cnt).revenue(rev).delta(d)
                .build();
        }).toList();
    }


    // =========================
    // Platform Revenue (DB 우선 + fallback)
    // =========================
    public long sumPlatformRevenue(String region, String hotel, LocalDate from, LocalDate to, double defaultCommission) {
        String sql = """
            SELECT COALESCE(SUM(
                     GREATEST(p.amount - COALESCE(rf.refunded_amount,0), 0)
                     * (CASE
                          WHEN h.settlement_fee_pct IS NULL THEN :defaultCommission
                          WHEN h.settlement_fee_pct > 1    THEN h.settlement_fee_pct / 100
                          ELSE h.settlement_fee_pct
                        END)
                   ), 0)
              FROM payments p
              JOIN bookings b ON p.booking_id = b.id
              JOIN hotels   h ON b.hotel_id   = h.id
         LEFT JOIN (
                SELECT r.payment_id, SUM(r.amount) AS refunded_amount
                  FROM refunds r
                 WHERE r.status = :succ
                 GROUP BY r.payment_id
              ) rf ON rf.payment_id = p.id
             WHERE p.status = :succ
               AND p.approved_at >= :fromTs
               AND p.approved_at <  :toTs
               AND (:region IS NULL OR h.region = :region)
               AND (:hotel  IS NULL OR (LOWER(h.name) LIKE LOWER(CONCAT('%', :hotel, '%'))
                                     OR CAST(h.id AS CHAR) = :hotel))
        """;

        Object result = em.createNativeQuery(sql)
                .setParameter("succ", SUCCEEDED)
                .setParameter("fromTs", java.sql.Timestamp.valueOf(from.atStartOfDay()))
                .setParameter("toTs",   java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()))
                .setParameter("region", emptyToNull(region))
                .setParameter("hotel",  emptyToNull(hotel))
                .setParameter("defaultCommission", defaultCommission)
                .getSingleResult();

        return ((Number) result).longValue();
    }

    // =========================
    // New Users (weekly, Monday week start) — 안전한 YEARWEEK 방식
    // =========================
    public int countNewUsersThisWeek() {
        String sql = """
            SELECT COUNT(*)
              FROM users u
             WHERE YEARWEEK(u.created_at, 1) = YEARWEEK(CURDATE(), 1)
        """;
        Object result = em.createNativeQuery(sql).getSingleResult();
        return ((Number) result).intValue();
    }

    public int countNewUsersLastWeek() {
        String sql = """
            SELECT COUNT(*)
              FROM users u
             WHERE YEARWEEK(u.created_at, 1) = YEARWEEK(DATE_SUB(CURDATE(), INTERVAL 1 WEEK), 1)
        """;
        Object result = em.createNativeQuery(sql).getSingleResult();
        return ((Number) result).intValue();
    }

    // =========================
    // helpers
    // =========================
    private String emptyToNull(String s){
        return (s==null || s.isBlank()) ? null : s;
    }

    private long daysBetween(LocalDate f, LocalDate t){
        // [f, t] 양끝 포함
        return Math.max(1, t.toEpochDay() - f.toEpochDay() + 1);
    }

    private double toDouble(Object o){
        return o==null ? 0.0 : ((Number)o).doubleValue();
    }
}
