// src/main/java/com/example/hotelres/settlement/SettlementController.java
package com.example.hotelres.settlement;

import com.example.hotelres.settlement.dto.*;
import com.example.hotelres.settlement.entity.SettlementStatement;
import com.example.hotelres.settlement.entity.enums.StatementStatus;
import com.example.hotelres.settlement.repo.SettlementStatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlements")
@Slf4j
public class SettlementController {

  private final SettlementQueryService query;
  private final SettlementCommandService command;
  private final SettlementStatementRepository stmtRepo;

  /* ========= 계산 조회 ========= */

  @GetMapping("/summary")
  public List<HotelSummaryDTO> summary(@RequestParam(required = false) Long hotelId,
                                       @RequestParam(required = false) Long roomTypeId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    log.info("[Settlement][API] GET /summary hotelId={}, roomTypeId={}, {}~{}", hotelId, roomTypeId, start, end);
    return query.summaryByHotel(hotelId, roomTypeId, start, end);
  }

  @GetMapping("/items")
  public List<SettlementLineDTO> items(@RequestParam(required = false) Long hotelId,
                                       @RequestParam(required = false) Long roomTypeId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    log.info("[Settlement][API] GET /items hotelId={}, roomTypeId={}, {}~{}", hotelId, roomTypeId, start, end);
    return query.lines(hotelId, roomTypeId, start, end);
  }

  /* ========= 정산서 생성/확정 ========= */

  @PostMapping("/generate")
  public SettlementStatement generate(@RequestBody GenerateRequest req) {
    log.info("[Settlement][API] POST /generate hotelId={}, {}~{}", req.hotelId(), req.start(), req.end());
    return command.generateWeekly(req.hotelId(), req.start(), req.end(),
                                  req.bankCode(), req.accountNo(), req.holderName());
  }

  @PostMapping("/generate-month")
  public List<SettlementStatement> generateMonth(@RequestParam Long hotelId,
                                                 @RequestParam String month,
                                                 @RequestParam String bankCode,
                                                 @RequestParam String accountNo,
                                                 @RequestParam String holderName) {
    log.info("[Settlement][API] POST /generate-month hotelId={}, month={}", hotelId, month);
    return command.generateForMonth(hotelId, month, bankCode, accountNo, holderName);
  }

  @PostMapping("/{id}/settle")
  public ResponseEntity<?> settle(@PathVariable Long id) {
      return stmtRepo.findById(id)
          .map(stmt -> {
              command.settle(id);
              return ResponseEntity.ok().build();
          })
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .body(Map.of("message","정산서가 존재하지 않습니다.")));
  }



  /* ========= 지난주 강제 정산 생성 + 확정 ========= */

  @PostMapping("/force-last-week-settle")
  public List<SettlementStatement> forceLastWeekSettle() {
    log.info("[Settlement][API] POST /force-last-week-settle");
    return command.generateAndSettleLastWeekForAllHotels();
  }

  /* ========= 정산서 조회 ========= */

  @GetMapping("/statements")
  public List<SettlementStatement> list(@RequestParam(required = false) Long hotelId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                        @RequestParam(required = false) StatementStatus status) {
    log.info("[Settlement][API] GET /statements hotelId={}, status={}, {}~{}", hotelId, status, start, end);
    if (hotelId == null) {
      return (status != null)
          ? stmtRepo.findByStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(status, start, end)
          : stmtRepo.findByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(start, end);
    }
    return (status != null)
        ? stmtRepo.findByHotelIdAndStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(hotelId, status, start, end)
        : stmtRepo.findByHotelIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(hotelId, start, end);
  }

  @GetMapping("/statements/settled")
  public List<SettlementStatement> settled(@RequestParam Long hotelId,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    log.info("[Settlement][API] GET /statements/settled hotelId={}, {}~{}", hotelId, start, end);
    return stmtRepo.findByHotelIdAndStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
        hotelId, StatementStatus.SETTLED, start, end);
  }

  /* ========= 지급계좌 기본값 ========= */

  @GetMapping("/payout-default")
  public PayoutInfoDTO payoutDefault(@RequestParam Long hotelId) {
    log.info("[Settlement][API] GET /payout-default hotelId={}", hotelId);
    return stmtRepo.findTop1ByHotelIdOrderByCreatedAtDesc(hotelId)
        .map(s -> new PayoutInfoDTO(s.getPayoutBankCode(), s.getPayoutAccountNo(), s.getPayoutHolderName()))
        .orElse(null);
  }

  // ✅ 프론트 호환(현재 프론트는 /api/hotels/{id}/payout 호출)
  @GetMapping("/hotels/{hotelId}/payout")
  public PayoutInfoDTO payoutDefaultCompat(@PathVariable Long hotelId) {
    log.info("[Settlement][API] GET /hotels/{}/payout (compat)", hotelId);
    return payoutDefault(hotelId);
  }

  /* ========= 공통 예외 매핑 ========= */

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Map<String, String> handleIllegalState(IllegalStateException e) {
    log.warn("[Settlement][API][409] {}", e.getMessage());
    return Map.of("message", e.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> handleIllegalArgument(IllegalArgumentException e) {
    log.warn("[Settlement][API][400] {}", e.getMessage());
    return Map.of("message", e.getMessage());
  }
  @GetMapping("/hotels/search")
  public List<HotelSearchDTO> searchHotels(@RequestParam String q) {
      log.info("[Settlement][API] GET /hotels/search q={}", q);
      return query.searchHotels(q);
  }

}
