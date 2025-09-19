package com.example.hotelres.hotel;

import com.example.hotelres.hotel.dto.HotelDetailsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class HotelDetailsService {
    private final HotelDetailsRepository repo;

    // ✅ guests 반영 버전
    public HotelDetailsDto getDetails(Long id, LocalDate ci, LocalDate co, Integer guests) {
        if (ci == null || co == null || !co.isAfter(ci)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        int g = (guests == null || guests < 1) ? 1 : guests; // 최소 1명 보정
        return repo.findDetails(id, ci, co, g);               // ✅ repo에 전달
    }

    // (선택) 하위호환: 기존 호출이 있으면 임시로 유지
    public HotelDetailsDto getDetails(Long id, LocalDate ci, LocalDate co) {
        return getDetails(id, ci, co, 1);
    }
}
