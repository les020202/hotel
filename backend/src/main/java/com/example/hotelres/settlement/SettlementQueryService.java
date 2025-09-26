// src/main/java/com/example/hotelres/settlement/SettlementQueryService.java
package com.example.hotelres.settlement;

import com.example.hotelres.settlement.dto.HotelSummaryDTO;
import com.example.hotelres.settlement.dto.SettlementLineDTO;
import com.example.hotelres.settlement.repo.SettlementCalcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SettlementQueryService {

  private final SettlementCalcRepository calcRepo;

  /** 라인 상세(호텔/객실타입 옵션, 기간 필수) */
  public List<SettlementLineDTO> lines(Long hotelId, Long roomTypeId, LocalDate start, LocalDate end) {
    validatePeriod(start, end);
    return calcRepo.calculateLines(hotelId, roomTypeId, start, end);
  }

  /** 호텔별 요약(호텔/객실타입 옵션, 기간 필수) */
  public List<HotelSummaryDTO> summaryByHotel(Long hotelId, Long roomTypeId, LocalDate start, LocalDate end) {
    validatePeriod(start, end);
    return calcRepo.summarizeByHotel(hotelId, roomTypeId, start, end);
  }

  /** 지급 대상 금액(Net 합계) — hotelId 단위(정산서는 호텔 단위이므로 필수) */
  public long computePayableAmount(Long hotelId, LocalDate start, LocalDate end) {
    validatePeriod(start, end);
    if (hotelId == null) {
      throw new IllegalArgumentException("hotelId is required for payable computation");
    }
    // roomTypeId = null → 전체 객실타입 집계
    List<HotelSummaryDTO> summaries = calcRepo.summarizeByHotel(hotelId, null, start, end);
    long total = sumNet(summaries);
    log.debug("[Settlement] computePayableAmount hotel={}, {}~{}, netSum={}", hotelId, start, end, total);
    return total;
  }

  /** (선택) 특정 객실타입만 집계하고 싶을 때 */
  public long computePayableAmount(Long hotelId, Long roomTypeId, LocalDate start, LocalDate end) {
    validatePeriod(start, end);
    if (hotelId == null) {
      throw new IllegalArgumentException("hotelId is required for payable computation");
    }
    List<HotelSummaryDTO> summaries = calcRepo.summarizeByHotel(hotelId, roomTypeId, start, end);
    long total = sumNet(summaries);
    log.debug("[Settlement] computePayableAmount hotel={}, roomTypeId={}, {}~{}, netSum={}",
        hotelId, roomTypeId, start, end, total);
    return total;
  }

  /* -------------------- 내부 헬퍼 -------------------- */

  private void validatePeriod(LocalDate start, LocalDate end) {
    Objects.requireNonNull(start, "start is required");
    Objects.requireNonNull(end,   "end is required");
    if (end.isBefore(start)) {
      throw new IllegalArgumentException("end must be on/after start");
    }
  }

  /** HotelSummaryDTO.netSum() 안전 합산 + 음수 방지 */
  private long sumNet(List<HotelSummaryDTO> summaries) {
    if (summaries == null || summaries.isEmpty()) return 0L;
    long sum = 0L;
    for (HotelSummaryDTO dto : summaries) {
      Long net = (dto == null ? null : dto.netSum());
      if (net != null) sum += net;
    }
    return Math.max(sum, 0L);
  }
}
