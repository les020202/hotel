package com.example.hotelres.admin;

import com.example.hotelres.admin.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AdminDashboardRepository repo;

    // DB가 NULL일 때만 사용하는 fallback 기본 수수료(비율)
    private static final double DEFAULT_COMMISSION = 0.12;

    public OverviewDto getOverview(String region, String hotel, LocalDate from, LocalDate to){
        // GMV / Refunds
        long gmvToday    = repo.sumGmvByDate(region, hotel, to, to);
        long gmvWeek     = repo.sumGmvByDate(region, hotel, to.minusDays(6), to);
        long gmvMonth    = repo.sumGmvByDate(region, hotel, to.withDayOfMonth(1), to);
        long refundMonth = repo.sumRefundByDate(region, hotel, to.withDayOfMonth(1), to);

        long gmvCustom   = repo.sumGmvByDate(region, hotel, from, to);
        long refundCustom= repo.sumRefundByDate(region, hotel, from, to);

        // KPI (최근 30일)
        LocalDate s30 = to.minusDays(29);
        long   adr30          = Math.round(repo.calcAdr(region, hotel, s30, to));
        double occ30          = repo.calcOccupancy(region, hotel, s30, to);
        long   revpar30       = Math.round(repo.calcRevpar(region, hotel, s30, to));
        double longStayRate30 = repo.calcLongstay(region, hotel, s30, to);

        // 증감률
        double todayVsYesterday = pctDelta(repo.sumGmvByDate(region, hotel, to.minusDays(1), to.minusDays(1)), gmvToday);
        double todayVsLastWeek  = pctDelta(repo.sumGmvByDate(region, hotel, to.minusDays(7), to.minusDays(7)), gmvToday);

        // 정산 스냅샷
        SettlementSnapshot ss = repo.settlementPending(region, hotel, from, to);

        // 플랫폼 수익 (DB 우선 + fallback)
        long platformRevenue = repo.sumPlatformRevenue(region, hotel, from, to, DEFAULT_COMMISSION);

        // 신규 유저 (이번주 / 지난주)
        int newUsersThisWeek = repo.countNewUsersThisWeek();
        int newUsersLastWeek = repo.countNewUsersLastWeek();

        // 증감률 계산
        double newUsersVsLastWeek = (newUsersLastWeek == 0)
            ? (newUsersThisWeek > 0 ? 1.0 : 0.0)
            : (newUsersThisWeek - newUsersLastWeek) / (double)newUsersLastWeek;

        return OverviewDto.builder()
                .gmvToday(gmvToday)
                .gmvWeek(gmvWeek)
                .gmvMonth(gmvMonth)
                .gmvCustom(gmvCustom)
                .refundMonth(refundMonth)
                .refundCustom(refundCustom)
                .adr30(adr30)
                .occ30(occ30)
                .revpar30(revpar30)
                .longStayRate30(longStayRate30)
                .todayVsYesterday(todayVsYesterday)
                .todayVsLastWeek(todayVsLastWeek)
                .settlementPending(ss.pending())
                .settlementDeltaVsPrevMonth(ss.deltaVsPrevMonth())
                .settlementPeriodText(ss.periodText())
                .settlementCount(ss.count())
                .platformRevenue(platformRevenue)
                // ✅ 신규유저 KPI
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersLastWeek(newUsersLastWeek)
                .newUsersVsLastWeek(newUsersVsLastWeek)
                .build();
    }

    private double pctDelta(long prev, long cur){
        if (prev == 0) return cur > 0 ? 1.0 : 0.0;
        return (cur - prev) / (double) prev;
    }

    // ---- Trends
    public TrendDto getTrendsDaily(String region, String hotel, LocalDate from, LocalDate to){
        List<BucketRow> rows = repo.dailyBuckets(region, hotel, from, to);
        return TrendDto.builder()
                .labels(rows.stream().map(BucketRow::label).toList())
                .revenue(rows.stream().map(BucketRow::gmv).toList())
                .bookings(rows.stream().map(BucketRow::bookings).toList())
                .build();
    }

    public TrendDto getTrendsHourly(String region, String hotel, LocalDate from, LocalDate to){
        List<BucketRow> rows = repo.hourlyBuckets(region, hotel, to); // to 날짜 기준
        return TrendDto.builder()
                .labels(rows.stream().map(BucketRow::label).toList())
                .revenue(rows.stream().map(BucketRow::gmv).toList())
                .bookings(rows.stream().map(BucketRow::bookings).toList())
                .build();
    }

    // ---- Rankings
    public Object getRankings(String range, String type, int minVolume, String region, String hotel, LocalDate from, LocalDate to){
        if ("refundRate".equals(type)) {
            return repo.refundRateRank(range, minVolume, region, hotel, from, to);
        } else {
            return repo.revenueRank(range, region, hotel, from, to);
        }
    }

    public List<RankingRow> getVelocity(String range, int limit, String region, String hotel, LocalDate from, LocalDate to){
        return repo.velocityRank(range, limit, region, hotel, from, to);
    }

    // ---- Decomposition
    public List<DecompRow> getDecomposition(String metric, String by, String region, String hotel, LocalDate from, LocalDate to){
        List<DecompRow> rows = switch (by) {
            case "region"  -> repo.decompByRegion(region, hotel, from, to);
            case "hotel"   -> repo.decompByHotel(region, hotel, from, to);
            case "channel" -> repo.decompByChannel(region, hotel, from, to);
            default -> List.of();
        };
        long total = rows.stream().mapToLong(DecompRow::getGmv).sum();
        if (total > 0) rows.forEach(r -> r.setShare(r.getGmv() / (double) total));
        return rows;
    }

    // ---- Records
    public record BucketRow(String label, long gmv, int bookings){}
    public record OverviewAggregates(double adr, double occ, double revpar, double longStayRate){}
    public record SettlementSnapshot(long pending, double deltaVsPrevMonth, String periodText, int count){}

    // ---- Proxy getters
    public AdminDashboardService.SettlementSnapshot getSettlement(String region, String hotel, LocalDate from, LocalDate to) {
        return repo.settlementPending(region, hotel, from, to);
    }

    public List<String> listRegions() { return repo.listRegions(); }
    // 필터 반영 버전
    public List<RankingRow> getTop10TodayRevenue(String region, String hotel) {
        return repo.top10TodayRevenue(region, hotel);
    }
    public List<RankingRow> getTop10MonthlyRevenue(String region, String hotel) {
        return repo.top10MonthlyRevenue(region, hotel);
    }

    // 하위 호환(기존 호출 유지)
    public List<RankingRow> getTop10TodayRevenue() {
        return getTop10TodayRevenue(null, null);
    }
    public List<RankingRow> getTop10MonthlyRevenue() {
        return getTop10MonthlyRevenue(null, null);
    }
}
