package com.example.hotelres.owner.status;

import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NowCheckInService {

    private final NowCheckInRepository repository;

    public long getTodayCheckedInCount(long hotelId) {
        // KST 기준 '오늘' 00:00 ~ 내일 00:00
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(KST);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end   = today.plusDays(1).atStartOfDay();
        return repository.countTodayCheckedInDistinct(hotelId, start, end);
    }
}
