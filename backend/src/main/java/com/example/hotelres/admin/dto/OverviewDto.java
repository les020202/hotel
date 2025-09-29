package com.example.hotelres.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OverviewDto {
    private long gmvToday;
    private long gmvWeek;
    private long gmvMonth;
    private long refundMonth;

    private long adr30;          // 평균 일일 요금 (최근 30일, 반올림)
    private double occ30;        // 점유율 (최근 30일)
    private long revpar30;       // RevPAR (최근 30일, 반올림)
    private double longStayRate30;

    private double todayVsYesterday;
    private double todayVsLastWeek;

    // Settlement snapshot
    private long   settlementPending;
    private double settlementDeltaVsPrevMonth;
    private String settlementPeriodText;
    private int    settlementCount;

    // ✅ 플랫폼 수익 (DB settlement_fee_pct 우선 반영)
    private long platformRevenue;
    
    
    private long gmvCustom;
    private long refundCustom;
    
    private int newUsersThisWeek;
    private int newUsersLastWeek;      // 지난주 수
    private double newUsersVsLastWeek; // 증감률
}
