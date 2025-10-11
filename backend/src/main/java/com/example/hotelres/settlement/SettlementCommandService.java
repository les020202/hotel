// src/main/java/com/example/hotelres/settlement/SettlementCommandService.java
package com.example.hotelres.settlement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final HotelRepository hotelRepo; // 전체 호텔 ID 조회용

    // ───────────────────────────── 공통 상수/헬퍼 ─────────────────────────────
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** periodEnd(일요일) 기준 다음 주 수요일 00:05 KST */
    private static ZonedDateTime finalizableAt(LocalDate periodEnd) {
        return periodEnd.plusDays(3).atStartOfDay(KST).plusMinutes(5); // 수 00:05
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) if (!isBlank(c)) return c;
        return "";
    }

    private void validatePeriod(Long hotelId, LocalDate start, LocalDate end) {
        Objects.requireNonNull(hotelId, "hotelId is required");
        Objects.requireNonNull(start, "start is required");
        Objects.requireNonNull(end, "end is required");
        if (end.isBefore(start)) throw new IllegalArgumentException("end must be on/after start");
    }

    private String[] resolvePayoutFallback(Long hotelId, String bankCode, String accountNo, String holderName) {
        // 1) 요청값이 모두 있으면 그대로
        if (!isBlank(bankCode) && !isBlank(accountNo) && !isBlank(holderName)) {
            return new String[]{ bankCode, accountNo, holderName };
        }
        // 2) 이 호텔의 최근 정산서의 지급정보
        Optional<SettlementStatement> lastStmtOpt = stmtRepo.findTop1ByHotelIdOrderByCreatedAtDesc(hotelId);
        String bc = firstNonBlank(bankCode,  lastStmtOpt.map(SettlementStatement::getPayoutBankCode).orElse(null),  "");
        String an = firstNonBlank(accountNo, lastStmtOpt.map(SettlementStatement::getPayoutAccountNo).orElse(null), "");
        String hn = firstNonBlank(holderName,lastStmtOpt.map(SettlementStatement::getPayoutHolderName).orElse(null),"");
        if (!isBlank(bc) && !isBlank(an) && !isBlank(hn)) {
            return new String[]{ bc, an, hn };
        }
        // 3) 호텔 기본 지급정보(lookup)
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

    // ───────────────────────────── 생성 계열 ─────────────────────────────
    /** 주간 생성(단일 호텔) - 멱등 */
    @Transactional
    public SettlementStatement generateWeekly(Long hotelId, LocalDate start, LocalDate end,
                                              String bankCode, String accountNo, String holderName) {
        validatePeriod(hotelId, start, end);
        log.info("[Settlement][GenerateWeekly] hotelId={}, period={}~{}", hotelId, start, end);

        String[] payout = resolvePayoutFallback(hotelId, bankCode, accountNo, holderName);
        final String bc = payout[0], an = payout[1], hn = payout[2];

        Optional<SettlementStatement> opt = stmtRepo.findByHotelIdAndPeriodStartAndPeriodEnd(hotelId, start, end);
        SettlementStatement stmt = opt.orElseGet(() -> {
            log.info("[Settlement][GenerateWeekly] create header: hotel={}, {}~{}", hotelId, start, end);
            return new SettlementStatement(hotelId, start, end, bc, an, hn);
        });

        // 지급정보 최신화
        stmt.setPayoutBankCode(bc);
        stmt.setPayoutAccountNo(an);
        stmt.setPayoutHolderName(hn);

        // 예정 금액 산정 (프리뷰)
        long payableDraft = queryService.computePayableAmount(hotelId, start, end);
        stmt.setPayableAmount(payableDraft);

        if (stmt.getStatus() == null) stmt.setStatus(StatementStatus.PLANNED);

        SettlementStatement saved = stmtRepo.save(stmt);
        log.info("[Settlement][GenerateWeekly] upsert: id={}, status={}, payableDraft={}",
                saved.getId(), saved.getStatus(), saved.getPayableAmount());
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
        log.info("[Settlement][GenerateForMonth] hotel={}, month={}, count={}", hotelId, month, list.size());
        return list;
    }

    /** 오늘 주차 단일 호텔 */
    @Transactional
    public SettlementStatement generateTodayWeekly(Long hotelId) {
        LocalDate today = LocalDate.now(KST);
        LocalDate start = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate end   = today.with(java.time.DayOfWeek.SUNDAY);
        return generateWeekly(hotelId, start, end, null, null, null);
    }

    /** 오늘 주차 전체 호텔 */
    @Transactional
    public List<SettlementStatement> generateTodayForAllHotels() {
        LocalDate today = LocalDate.now(KST);
        LocalDate start = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate end   = today.with(java.time.DayOfWeek.SUNDAY);

        List<Long> hotelIds = hotelRepo.findAllHotelIds();
        List<SettlementStatement> results = new ArrayList<>();
        log.info("[Settlement][GenerateTodayAll] {}~{}, hotelCount={}", start, end, hotelIds.size());

        for (Long hotelId : hotelIds) {
            try {
                SettlementStatement stmt = generateWeekly(hotelId, start, end, null, null, null);
                results.add(stmt);
            } catch (Exception e) {
                log.error("[Settlement][GenerateTodayAll] failed hotelId={}, err={}", hotelId, e.toString());
            }
        }
        log.info("[Settlement][GenerateTodayAll] done, upsertCount={}", results.size());
        return results;
    }

    /** 지난주 단일 호텔 정산 생성 */
    @Transactional
    public SettlementStatement generateLastWeekWeekly(Long hotelId) {
        LocalDate today = LocalDate.now(KST);
        LocalDate start = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate end   = today.minusWeeks(1).with(java.time.DayOfWeek.SUNDAY);
        return generateWeekly(hotelId, start, end, null, null, null);
    }

    /** 지난주 전체 호텔 정산 생성 */
    @Transactional
    public List<SettlementStatement> generateLastWeekForAllHotels() {
        LocalDate today = LocalDate.now(KST);
        LocalDate start = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate end   = today.minusWeeks(1).with(java.time.DayOfWeek.SUNDAY);

        List<Long> hotelIds = hotelRepo.findAllHotelIds();
        List<SettlementStatement> results = new ArrayList<>();
        log.info("[Settlement][GenerateLastWeekAll] {}~{}, hotelCount={}", start, end, hotelIds.size());

        for (Long hotelId : hotelIds) {
            try {
                results.add(generateWeekly(hotelId, start, end, null, null, null));
            } catch (Exception e) {
                log.error("[Settlement][GenerateLastWeekAll] failed hotelId={}, err={}", hotelId, e.toString());
            }
        }
        log.info("[Settlement][GenerateLastWeekAll] done, upsertCount={}", results.size());
        return results;
    }

    // ───────────────────────────── 스케줄러 ─────────────────────────────
    /** 매주 월요일 02:00 KST — 이번 주 PLANNED 자동 생성 */
    @Scheduled(cron = "0 0 2 ? * MON", zone = "Asia/Seoul")
    public void scheduledWeeklySettlement() {
        log.info("[Settlement][Scheduler][Mon-02:00] START generate PLANNED for THIS week");
        generateTodayForAllHotels();
        log.info("[Settlement][Scheduler][Mon-02:00] END");
    }

    /** 매주 금요일 00:05 KST — 지난주 PLANNED 누락분 자동 확정(운영 누락 방지) */
    @Scheduled(cron = "0 5 0 ? * FRI", zone = "Asia/Seoul")
    public void scheduledAutoFinalizeSweepOnFriday() {
        log.info("[Settlement][Scheduler][Fri-00:05] START auto finalize sweep");
        sweepFinalizeLastWeek("Fri-00:05");
        log.info("[Settlement][Scheduler][Fri-00:05] END");
    }

    /** 지난주 statement 생성 보장 후, PLANNED만 확정(컷오프 가드 내장) */
    private void sweepFinalizeLastWeek(String tag) {
        LocalDate lastSun = LocalDate.now(KST).with(java.time.DayOfWeek.SUNDAY).minusWeeks(1);
        LocalDate lastMon = lastSun.minusDays(6);
        ZonedDateTime cutoff = finalizableAt(lastSun);
        ZonedDateTime now = ZonedDateTime.now(KST);

        log.info("[Settlement][Sweep:{}] target={}~{}, cutoff={}, now={}", tag, lastMon, lastSun, cutoff, now);
        if (now.isBefore(cutoff)) {
            log.info("[Settlement][Sweep:{}] skip(before cutoff)", tag);
            return;
        }

        List<Long> hotelIds = hotelRepo.findAllHotelIds();
        int foundOrCreated = 0, finalized = 0, failed = 0;

        for (Long hotelId : hotelIds) {
            try {
                SettlementStatement stmt = stmtRepo
                        .findByHotelIdAndPeriodStartAndPeriodEnd(hotelId, lastMon, lastSun)
                        .orElseGet(() -> generateWeekly(hotelId, lastMon, lastSun, null, null, null));
                foundOrCreated++;

                if (stmt.getStatus() == StatementStatus.PLANNED) {
                    settle(stmt.getId()); // 내부에서 cutoff/재집계 처리
                    finalized++;
                }
            } catch (Exception e) {
                failed++;
                log.error("[Settlement][Sweep:{}] hotelId={} finalize fail: {}", tag, hotelId, e.toString());
            }
        }
        log.info("[Settlement][Sweep:{}] result foundOrCreated={}, finalized={}, failed={}",
                tag, foundOrCreated, finalized, failed);
    }

    // ───────────────────────────── 확정/지급 ─────────────────────────────
    /** 관리자 확정 — 다음 주 수요일 00:05(KST) 이후만 허용 */
    @Transactional
    public void settle(Long id) {
        SettlementStatement stmt = stmtRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("SettlementStatement not found: " + id));

        log.info("[Settlement][Settle] req id={}, hotel={}, period={}~{}, status={}",
                stmt.getId(), stmt.getHotelId(), stmt.getPeriodStart(), stmt.getPeriodEnd(), stmt.getStatus());

        if (stmt.getStatus() == StatementStatus.SETTLED) {
            log.info("[Settlement][Settle] already SETTLED: id={}", stmt.getId());
            return;
        }
        if (stmt.getStatus() != StatementStatus.PLANNED) {
            throw new IllegalStateException("Cannot settle from status: " + stmt.getStatus());
        }

        // 컷오프 가드
        ZonedDateTime cutoff = finalizableAt(stmt.getPeriodEnd());
        ZonedDateTime now = ZonedDateTime.now(KST);
        if (now.isBefore(cutoff)) {
            String msg = String.format("정산 확정은 %s 이후 가능합니다. (현재: %s)", cutoff, now);
            throw new CutoffNotReachedException(msg);  // ✅ 생성자도 message 하나만 받도록 변경
        }

        // 컷오프 이전 온라인 결제 기준 재집계
        long payable;
        try {
            payable = queryService.computePayableAmountBeforeCutoffOnlineOnly(
                    stmt.getHotelId(), stmt.getPeriodStart(), stmt.getPeriodEnd(), cutoff.toLocalDateTime());
        } catch (Throwable t) {
            log.warn("[Settlement][Settle] fallback to computePayableAmount(): {}", t.toString());
            payable = queryService.computePayableAmount(
                    stmt.getHotelId(), stmt.getPeriodStart(), stmt.getPeriodEnd());
        }

        stmt.setPayableAmount(payable);
        stmt.setStatus(StatementStatus.SETTLED);
        stmt.setSettledAt(LocalDateTime.now(KST));
        SettlementStatement saved = stmtRepo.save(stmt);

        // 지급 트랜잭션 생성(PENDING)
        PayoutTransaction tx = PayoutTransaction.builder()
                .statementId(saved.getId())
                .requestedAmount(saved.getPayableAmount())
                .status(PayoutStatus.PENDING)
                .requestedAt(LocalDateTime.now(KST))
                .build();
        payoutRepo.save(tx);

        log.info("[Settlement][Settle] SETTLED: id={}, hotel={}, period={}~{}, amount={}, payoutStatus={}",
                saved.getId(), saved.getHotelId(), saved.getPeriodStart(), saved.getPeriodEnd(),
                saved.getPayableAmount(), PayoutStatus.PENDING);
    }


    /** 지난주 전체 호텔 정산 강제 생성 + 확정(운영 배치용) — 컷오프 가드로 수요일 이전엔 예외 */
    @Transactional
    public List<SettlementStatement> generateAndSettleLastWeekForAllHotels() {
        LocalDate today = LocalDate.now(KST);
        LocalDate start = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate end   = today.minusWeeks(1).with(java.time.DayOfWeek.SUNDAY);
        ZonedDateTime cutoff = finalizableAt(end);

        List<Long> hotelIds = hotelRepo.findAllHotelIds();
        List<SettlementStatement> results = new ArrayList<>();

        log.info("[Settlement][Gen+SettleLastWeekAll] START {}~{}, cutoff={}", start, end, cutoff);
        int ok = 0, fail = 0;

        for (Long hotelId : hotelIds) {
            try {
                SettlementStatement stmt = generateWeekly(hotelId, start, end, null, null, null);
                settle(stmt.getId()); // 컷오프 이전이면 여기서 예외
                results.add(stmt);
                ok++;
            } catch (Exception e) {
                fail++;
                log.error("[Settlement][Gen+SettleLastWeekAll] failed hotelId={}, err={}", hotelId, e.toString());
            }
        }
        log.info("[Settlement][Gen+SettleLastWeekAll] END success={}, fail={}, total={}", ok, fail, results.size());
        return results;
    }
}
