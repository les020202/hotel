package com.example.hotelres.owner.status;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TodayCheckInService {

    private final TodayCheckInRepository repository;

    public long getTodayCount(long hotelId) {
        // KST 기준의 '오늘' (서비스 필요에 맞게 조정 가능)
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate tomorrow = today.plusDays(1);
        return repository.countToday(hotelId, today, tomorrow);
    }
}
