package com.example.hotelres.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.hotelres.main.repo.BookingDayRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class HotelPriceService {

    private final BookingDayRepository bookingDayRepository;

    /** 호텔 ID 집합 -> 최저가 맵 */
    public Map<Long, Integer> minPrices(Set<Long> hotelIds) {
        if (hotelIds == null || hotelIds.isEmpty()) return Collections.emptyMap();
        var rows = bookingDayRepository.findMinPriceByHotelIds(hotelIds);
        Map<Long, Integer> map = new HashMap<>();
        for (var r : rows) {
            map.put(r.getHotelId(), r.getMinPrice());
        }
        return map;
    }
}
