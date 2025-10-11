package com.example.hotelres.owner.status;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RemainingRoomsService {

    private final RemainingRoomsRepository repository;

    public long getTodayRemaining(long hotelId) {
        // 서비스 기준 타임존 (필요시 변경)
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        return repository.sumTodayRemaining(hotelId, today);
    }
}
