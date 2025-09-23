package com.example.hotelres.search;

import com.example.hotelres.common.dto.PagedResponse;
import com.example.hotelres.search.dto.HotelCardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository repo;

    // ✅ guests 추가
    public PagedResponse<HotelCardDto> search(LocalDate checkIn,
                                              LocalDate checkOut,
                                              String region,
                                              Integer guests,   // <-- 추가
                                              Integer limit,
                                              Integer offset) {
        int g = (guests == null || guests < 1) ? 1 : guests;     // ✅ 최소 1명 보정
        int pageSize = (limit == null || limit <= 0) ? 5 : limit;
        int off = (offset == null || offset < 0) ? 0 : offset;

        // ✅ 리포지토리 호출 시 guests 전달
        List<HotelCardDto> items = repo.findHotels(checkIn, checkOut, region, g, pageSize, off);
        long total = repo.countHotels(checkIn, checkOut, region, g);

        int nextOff = off + items.size();
        boolean hasMore = nextOff < total;

        return PagedResponse.<HotelCardDto>builder()
                .items(items)
                .limit(pageSize)
                .offset(off)
                .total(total)
                .hasMore(hasMore)
                .nextOffset(hasMore ? nextOff : null)
                .build();
    }
}
