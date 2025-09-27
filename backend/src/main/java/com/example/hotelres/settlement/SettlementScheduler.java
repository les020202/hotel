/*
 * // src/main/java/com/example/hotelres/settlement/SettlementScheduler.java
 * package com.example.hotelres.settlement;
 * 
 * import java.time.DayOfWeek; import java.time.LocalDate; import
 * java.util.List;
 * 
 * import org.springframework.scheduling.annotation.Scheduled; import
 * org.springframework.stereotype.Component;
 * 
 * import com.example.hotelres.admin.hotel.Hotel; import
 * com.example.hotelres.admin.hotel.HotelRepository;
 * 
 * import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
 * 
 * @Component
 * 
 * @RequiredArgsConstructor
 * 
 * @Slf4j public class SettlementScheduler {
 * 
 * private final SettlementCommandService command; private final HotelRepository
 * hotelRepo; // ✅ 전체 호텔 불러오기용
 * 
 *//** 매주 월요일 새벽 2시에 전체 호텔 정산 *//*
								 * @Scheduled(cron = "0 0 2 * * MON", zone = "Asia/Seoul") public void
								 * generateWeeklyForAllHotels() { LocalDate today = LocalDate.now(); LocalDate
								 * start = today.with(DayOfWeek.MONDAY); LocalDate end =
								 * today.with(DayOfWeek.SUNDAY);
								 * 
								 * List<Hotel> hotels = hotelRepo.findAll(); for (Hotel hotel : hotels) { try {
								 * command.generateWeekly(hotel.getId(), start, end, hotel.getPayoutBankCode(),
								 * hotel.getPayoutAccountNo(), hotel.getPayoutHolderName());
								 * log.info("[Scheduler] 정산 생성 완료: hotelId={}, 기간={}~{}", hotel.getId(), start,
								 * end); } catch (Exception e) { log.error("[Scheduler] 정산 생성 실패: hotelId={}",
								 * hotel.getId(), e); } } } }
								 */