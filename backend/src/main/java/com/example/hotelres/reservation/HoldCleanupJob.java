// path: src/main/java/com/example/hotelres/reservation/HoldCleanupJob.java
package com.example.hotelres.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoldCleanupJob {
    private final ReservationService reservationService;

    // 매 2분마다 만료 홀드 정리
    @Scheduled(cron = "0 */2 * * * *")
    public void run() {
        int released = reservationService.releaseExpiredHolds();
        if (released > 0) log.info("Released expired holds: {}", released);
    }
}
