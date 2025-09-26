package com.example.hotelres.owner.status;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReserveThisWeekService {

    private final ReserveThisWeekRepository repository;

    public long getWeeklyCount(long hotelId) {
        return repository.countThisWeek(hotelId);
    }
}
