package com.example.hotelres.admin;

import com.example.hotelres.admin.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService service;

    @GetMapping("/regions")
    public List<String> listRegions() {
        return service.listRegions();
    }

    // Overview
    @GetMapping("/overview")
    public ResponseEntity<OverviewDto> overview(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String hotel,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(service.getOverview(region, hotel, from, to));
    }

    // Daily trends
    @GetMapping("/trends")
    public ResponseEntity<TrendDto> trendsDaily(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String hotel,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to
    ){
        return ResponseEntity.ok(service.getTrendsDaily(region, hotel, from, to));
    }

    // Hourly trends (by 'to' date)
    @GetMapping("/trends-hourly")
    public ResponseEntity<TrendDto> trendsHourly(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String hotel,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to
    ){
        return ResponseEntity.ok(service.getTrendsHourly(region, hotel, from, to));
    }

    // Rankings
    @GetMapping("/rankings")
    public ResponseEntity<?> rankings(
            @RequestParam(defaultValue = "day") String range,
            @RequestParam(defaultValue = "revenue") String type,
            @RequestParam(defaultValue = "10") int minVolume,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String hotel,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to
    ){
        Object body = service.getRankings(range, type, minVolume, region, hotel, from, to);
        return ResponseEntity.ok(body);
    }

    // Velocity
    @GetMapping("/rankings/velocity")
    public ResponseEntity<List<RankingRow>> velocity(
            @RequestParam(defaultValue = "week") String range,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String hotel,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to
    ){
        return ResponseEntity.ok(service.getVelocity(range, limit, region, hotel, from, to));
    }

    // Decomposition
    @GetMapping("/decomp")
    public ResponseEntity<DecompositionResponse> decomp(
            @RequestParam(defaultValue = "gmv") String metric,
            @RequestParam(defaultValue = "region") String by,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String hotel,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to
    ){
        List<DecompRow> rows = service.getDecomposition(metric, by, region, hotel, from, to);
        return ResponseEntity.ok(new DecompositionResponse(rows));
    }

    @GetMapping("/top10/today")
    public List<RankingRow> top10TodayRevenue(
            @RequestParam(required = false) String region,
            @RequestParam(required = false, name = "hotel") String hotel // 호텔명 또는 ID 문자열
    ) {
        return service.getTop10TodayRevenue(region, hotel);
    }

    @GetMapping("/top10/monthly")
    public List<RankingRow> top10MonthlyRevenue(
            @RequestParam(required = false) String region,
            @RequestParam(required = false, name = "hotel") String hotel
    ) {
        return service.getTop10MonthlyRevenue(region, hotel);
    }

    // wrapper
    public record DecompositionResponse(List<DecompRow> rows) {}
}
