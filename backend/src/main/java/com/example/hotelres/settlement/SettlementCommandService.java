// src/main/java/com/example/hotelres/settlement/SettlementCommandService.java
package com.example.hotelres.settlement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.hotelres.admin.hotel.HotelRepository;
import com.example.hotelres.settlement.entity.PayoutTransaction;
import com.example.hotelres.settlement.entity.SettlementStatement;
import com.example.hotelres.settlement.entity.enums.PayoutStatus;
import com.example.hotelres.settlement.entity.enums.StatementStatus;
import com.example.hotelres.settlement.repo.HotelPayoutLookupRepository;
import com.example.hotelres.settlement.repo.PayoutTransactionRepository;
import com.example.hotelres.settlement.repo.SettlementStatementRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementCommandService {

    private final SettlementStatementRepository stmtRepo;
    private final PayoutTransactionRepository payoutRepo;
    private final SettlementQueryService queryService;
    private final HotelPayoutLookupRepository hotelPayoutLookupRepo;
    private final HotelRepository hotelRepo;   // ✅ 전체 호텔 조회용

    /** 주간 생성(단일 호텔) */
    @Transactional
    public SettlementStatement generateWeekly(Long hotelId, LocalDate start, LocalDate end,
                                              String bankCode, String accountNo, String holderName) {
        validatePeriod(hotelId, start, end);

        // 지급정보 보완
        String[] payout = resolvePayoutFallback(hotelId, bankCode, accountNo, holderName);
        final String bc = payout[0];
        final String an = payout[1];
        final String hn = payout[2];

        // 유니크 보호
        Optional<SettlementStatement> opt = stmtRepo.findByHotelIdAndPeriodStartAndPeriodEnd(hotelId, start, end);
        SettlementStatement stmt = opt.orElseGet(() ->
                new SettlementStatement(hotelId, start, end, bc, an, hn)
        );

        stmt.setPayoutBankCode(bc);
        stmt.setPayoutAccountNo(an);
        stmt.setPayoutHolderName(hn);

        // 금액 산정
        long payable = queryService.computePayableAmount(hotelId, start, end);
        stmt.setPayableAmount(payable);

        if (stmt.getStatus() == null) {
            stmt.setStatus(StatementStatus.PLANNED);
        }

        SettlementStatement saved = stmtRepo.save(stmt);
        log.info("[Settlement] generated weekly: hotel={}, {}~{}, payable={}, status={}",
                hotelId, start, end, saved.getPayableAmount(), saved.getStatus());
        return saved;
    }

    /** 특정 월 전체 주차 생성(단일 호텔) */
    @Transactional
    public List<SettlementStatement> generateForMonth(Long hotelId, String month,
                                                      String bankCode, String accountNo, String holderName) {
        Objects.requireNonNull(hotelId, "hotelId is required");
        YearMonth ym = YearMonth.parse(month);
        LocalDate first = ym.atDay(1);
        LocalDate last  = ym.atEndOfMonth();

        List<SettlementStatement> list = new ArrayList<>();
        LocalDate cursor = first.with(java.time.DayOfWeek.MONDAY);
        if (cursor.isAfter(first)) cursor = cursor.minusWeeks(1);

        while (!cursor.isAfter(last)) {
            LocalDate s = cursor;
            LocalDate e = cursor.with(java.time.DayOfWeek.SUNDAY);
            if (e.isAfter(last)) e = last;

            list.add(generateWeekly(hotelId, s, e, bankCode, accountNo, holderName));
            cursor = cursor.plusWeeks(1);
        }
        log.info("[Settlement] month generated: hotel={}, month={}, count={}", hotelId, month, list.size());
        return list;
    }

    /** 오늘 주차 단일 호텔 */
    @Transactional
    public SettlementStatement generateTodayWeekly(Long hotelId) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate end   = today.with(java.time.DayOfWeek.SUNDAY);
        return generateWeekly(hotelId, start, end, null, null, null);
    }

    /** 오늘 주차 전체 호텔 */
    @Transactional
    public List<SettlementStatement> generateTodayForAllHotels() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate end   = today.with(java.time.DayOfWeek.SUNDAY);

        List<Long> hotelIds = hotelRepo.findAllHotelIds();
        List<SettlementStatement> results = new ArrayList<>();

        for (Long hotelId : hotelIds) {
            try {
                SettlementStatement stmt = generateWeekly(hotelId, start, end, null, null, null);
                results.add(stmt);
            } catch (Exception e) {
                log.error("[Settlement] 호텔 {} 정산 생성 실패: {}", hotelId, e.getMessage());
            }
        }

        log.info("[Settlement] 오늘 주차 전체 호텔 정산 생성 완료: count={}", results.size());
        return results;
    }

    /** 지난주 단일 호텔 정산 강제 생성 */
    @Transactional
    public SettlementStatement generateLastWeekWeekly(Long hotelId) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate end   = today.minusWeeks(1).with(java.time.DayOfWeek.SUNDAY);
        return generateWeekly(hotelId, start, end, null, null, null);
    }

    /** 지난주 전체 호텔 정산 강제 생성 */
    @Transactional
    public List<SettlementStatement> generateLastWeekForAllHotels() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate end   = today.minusWeeks(1).with(java.time.DayOfWeek.SUNDAY);

        List<Long> hotelIds = hotelRepo.findAllHotelIds();
        List<SettlementStatement> results = new ArrayList<>();

        for (Long hotelId : hotelIds) {
            try {
                SettlementStatement stmt = generateWeekly(hotelId, start, end, null, null, null);
                results.add(stmt);
            } catch (Exception e) {
                log.error("[Settlement] 지난주 호텔 {} 정산 생성 실패: {}", hotelId, e.getMessage());
            }
        }

        log.info("[Settlement] 지난주 전체 호텔 정산 생성 완료: count={}", results.size());
        return results;
    }

    /** 매주 월요일 새벽 2시에 전체 호텔 정산 자동 생성 */
    @Scheduled(cron = "0 0 2 ? * MON")
    public void scheduledWeeklySettlement() {
        log.info("[Settlement][Scheduler] 매주 월요일 정산 자동 실행 시작");
        generateTodayForAllHotels();
    }

    /** 관리자 확정 */
    @Transactional
    public void settle(Long id) {
        SettlementStatement stmt = stmtRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SettlementStatement not found: " + id));

        if (stmt.getStatus() == StatementStatus.SETTLED) {
            return;
        }
        if (stmt.getStatus() != StatementStatus.PLANNED) {
            throw new IllegalStateException("Cannot settle from status: " + stmt.getStatus());
        }

        stmt.setStatus(StatementStatus.SETTLED);
        stmt.setSettledAt(LocalDateTime.now());
        if (stmt.getPayableAmount() == null) stmt.setPayableAmount(0L);
        stmtRepo.save(stmt);

        PayoutTransaction tx = PayoutTransaction.builder()
                .statementId(stmt.getId())
                .requestedAmount(stmt.getPayableAmount())
                .status(PayoutStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();
        payoutRepo.save(tx);

        log.info("[Settlement] settled: statementId={}, hotel={}, period={}~{}, amount={}",
                stmt.getId(), stmt.getHotelId(), stmt.getPeriodStart(), stmt.getPeriodEnd(), stmt.getPayableAmount());
    }

    /** 데모: 27번 호텔 */
    @Transactional
    public SettlementStatement generateDemoWeekly() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate end   = today.with(java.time.DayOfWeek.SUNDAY);
        return generateWeekly(27L, start, end, "004", "123-456-789012", "(주)호텔스냅");
    }

    /* 내부 헬퍼 */
    private void validatePeriod(Long hotelId, LocalDate start, LocalDate end) {
        Objects.requireNonNull(hotelId, "hotelId is required");
        Objects.requireNonNull(start, "start is required");
        Objects.requireNonNull(end, "end is required");
        if (end.isBefore(start)) throw new IllegalArgumentException("end must be on/after start");
    }

    private String[] resolvePayoutFallback(Long hotelId, String bankCode, String accountNo, String holderName) {
        if (!isBlank(bankCode) && !isBlank(accountNo) && !isBlank(holderName)) {
            return new String[]{ bankCode, accountNo, holderName };
        }
        Optional<SettlementStatement> lastStmtOpt = stmtRepo.findTop1ByHotelIdOrderByCreatedAtDesc(hotelId);
        String bc = firstNonBlank(bankCode,  lastStmtOpt.map(SettlementStatement::getPayoutBankCode).orElse(null),  "");
        String an = firstNonBlank(accountNo, lastStmtOpt.map(SettlementStatement::getPayoutAccountNo).orElse(null), "");
        String hn = firstNonBlank(holderName,lastStmtOpt.map(SettlementStatement::getPayoutHolderName).orElse(null),"");

        if (!isBlank(bc) && !isBlank(an) && !isBlank(hn)) {
            return new String[]{ bc, an, hn };
        }

        return hotelPayoutLookupRepo.findPayoutByHotelId(hotelId)
                .map(v -> new String[]{
                        firstNonBlank(bc, v.getPayoutBankCode(),  ""),
                        firstNonBlank(an, v.getPayoutAccountNo(), ""),
                        firstNonBlank(hn, v.getPayoutHolderName(),"")
                })
                .orElseGet(() -> {
                    log.warn("[Settlement] payout fallback not found: hotel={}, using blanks", hotelId);
                    return new String[]{ firstNonBlank(bc, ""), firstNonBlank(an, ""), firstNonBlank(hn, "") };
                });
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (!isBlank(c)) return c;
        }
        return "";
    }
    /** 지난주 전체 호텔 정산 강제 생성 + 확정 */
    @Transactional
    public List<SettlementStatement> generateAndSettleLastWeekForAllHotels() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate end   = today.minusWeeks(1).with(java.time.DayOfWeek.SUNDAY);

        List<Long> hotelIds = hotelRepo.findAllHotelIds();
        List<SettlementStatement> results = new ArrayList<>();

        for (Long hotelId : hotelIds) {
            try {
                SettlementStatement stmt = generateWeekly(hotelId, start, end, null, null, null);
                // ✅ 생성 직후 확정 처리
                settle(stmt.getId());
                results.add(stmt);
            } catch (Exception e) {
                log.error("[Settlement] 지난주 호텔 {} 정산 생성/확정 실패: {}", hotelId, e.getMessage());
            }
        }

        log.info("[Settlement] 지난주 전체 호텔 정산 생성+확정 완료: count={}", results.size());
        return results;
    }

}
