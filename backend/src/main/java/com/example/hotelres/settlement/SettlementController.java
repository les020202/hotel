// src/main/java/com/example/hotelres/settlement/SettlementController.java
package com.example.hotelres.settlement;

import com.example.hotelres.settlement.dto.GenerateRequest;
import com.example.hotelres.settlement.dto.HotelSummaryDTO;
import com.example.hotelres.settlement.dto.PayoutInfoDTO;
import com.example.hotelres.settlement.dto.SettlementLineDTO;
import com.example.hotelres.settlement.entity.SettlementStatement;
import com.example.hotelres.settlement.entity.enums.StatementStatus;
import com.example.hotelres.settlement.repo.SettlementStatementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlements")
public class SettlementController {

  private final SettlementQueryService query;
  private final SettlementCommandService command;
  private final SettlementStatementRepository stmtRepo;

  /* ========= 계산 조회 ========= */

  // 호텔별 요약
  @GetMapping("/summary")
  public List<HotelSummaryDTO> summary(@RequestParam(required = false) Long hotelId,
                                       @RequestParam(required = false) Long roomTypeId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    return query.summaryByHotel(hotelId, roomTypeId, start, end);
  }

  // 라인 상세
  @GetMapping("/items")
  public List<SettlementLineDTO> items(@RequestParam(required = false) Long hotelId,
                                       @RequestParam(required = false) Long roomTypeId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    return query.lines(hotelId, roomTypeId, start, end);
  }

  /* ========= 정산서 생성/확정 ========= */

  // 주간 생성
  @PostMapping("/generate")
  public SettlementStatement generate(@RequestBody GenerateRequest req) {
    return command.generateWeekly(
        req.hotelId(), req.start(), req.end(),
        req.bankCode(), req.accountNo(), req.holderName()
    );
  }

  // 데모용 주간 생성 (프론트 버튼)
  @PostMapping("/generate-demo-weekly")
  public SettlementStatement generateDemoWeekly() {
    return command.generateDemoWeekly();
  }

  // 특정 월(yyyy-MM)의 모든 주차 생성
  @PostMapping("/generate-month")
  public List<SettlementStatement> generateMonth(@RequestParam Long hotelId,
                                                 @RequestParam String month,
                                                 @RequestParam String bankCode,
                                                 @RequestParam String accountNo,
                                                 @RequestParam String holderName) {
    return command.generateForMonth(hotelId, month, bankCode, accountNo, holderName);
  }

  // 정산 확정(SETTLED 전환 + 지급로그 기록)
  @PostMapping("/{id}/settle")
  public void settle(@PathVariable Long id) {
    command.settle(id);
  }

  /* ========= 지난주 강제 정산 생성 + 확정 ========= */

  // 전체 호텔 지난주 정산 강제 생성 + 확정
  @PostMapping("/force-last-week-settle")
  public List<SettlementStatement> forceLastWeekSettle() {
    return command.generateAndSettleLastWeekForAllHotels();
  }

  /* ========= 정산서 조회 ========= */

  // 정산서 목록: hotelId 없을 때도 전체 조회 가능 + status 필터 지원
  @GetMapping("/statements")
  public List<SettlementStatement> list(@RequestParam(required = false) Long hotelId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                        @RequestParam(required = false) StatementStatus status) {
    if (hotelId == null) {
      if (status != null) {
        return stmtRepo.findByStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(status, start, end);
      }
      return stmtRepo.findByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(start, end);
    }
    if (status != null) {
      return stmtRepo.findByHotelIdAndStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
          hotelId, status, start, end);
    }
    return stmtRepo.findByHotelIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(hotelId, start, end);
  }

  // (옵션) 과거 호환: 완료(SETTLED)만
  @GetMapping("/statements/settled")
  public List<SettlementStatement> settled(@RequestParam Long hotelId,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    return stmtRepo.findByHotelIdAndStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
        hotelId, StatementStatus.SETTLED, start, end);
  }

  /* ========= 지급계좌 기본값 ========= */

  // 최근 생성된 정산서에서 지급정보 스냅샷을 기본값으로 제공
  @GetMapping("/payout-default")
  public PayoutInfoDTO payoutDefault(@RequestParam Long hotelId) {
    return stmtRepo.findTop1ByHotelIdOrderByCreatedAtDesc(hotelId)
        .map(s -> new PayoutInfoDTO(
            s.getPayoutBankCode(),
            s.getPayoutAccountNo(),
            s.getPayoutHolderName()))
        .orElse(null);
  }
}
