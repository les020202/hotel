package com.example.hotelres.settlement;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.example.hotelres.owner.HotelOwnerRepository;

import lombok.RequiredArgsConstructor;

//src/main/java/com/example/hotelres/settlement/OwnerSettlementQueryService.java
@Service
@RequiredArgsConstructor
public class OwnerSettlementQueryService {

 private static final ZoneId KST = ZoneId.of("Asia/Seoul");

 private final SettlementStatementSumRepository stmtRepo;
 private final HotelOwnerRepository hotelOwnerRepo; // existsByHotelIdAndUserLoginId 필요

 public WeeklySettlementDTO getLastWeekSettledForOwner(Long hotelId, String loginId) {
     // 1) 소유권 검사
     boolean owns = hotelOwnerRepo.existsByHotelIdAndUserLoginId(hotelId, loginId);
     if (!owns) {
         throw new org.springframework.security.access.AccessDeniedException("Not your hotel");
     }

     // 2) 지난주 범위(KST)
     LocalDate today = LocalDate.now(KST);
     LocalDate start = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
     LocalDate end   = today.minusWeeks(1).with(java.time.DayOfWeek.SUNDAY);

     // 3) 합계 조회(Native)
     Long sum = stmtRepo.sumSettledBetween(hotelId, start, end);
     if (sum == null) sum = 0L;

     return new WeeklySettlementDTO(hotelId, sum);
 }
}
