
// src/main/java/com/example/hotelres/search/SearchController.java

package com.example.hotelres.search;

import com.example.hotelres.common.dto.PagedResponse;
import com.example.hotelres.search.dto.HotelCardDto;

import com.example.hotelres.search.dto.HotelSuggestItem;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService service;


    /**
     * v3: 통합 검색 + 필터(가격/성급/별점밴드/어메니티ID)
     */

    @GetMapping("/hotels")
    public PagedResponse<HotelCardDto> hotels(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false, defaultValue = "rating") String sort,

            // 공통
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Integer adults,
            @RequestParam(required = false) Integer children,

            // ✅ 가격: 새/구 이름 모두 허용
            @RequestParam(required = false) Long priceMin,
            @RequestParam(required = false) Long priceMax,
            @RequestParam(required = false) Long minPrice,   // legacy
            @RequestParam(required = false) Long maxPrice,   // legacy

            // ✅ 성급: 새/구 이름 모두 허용 (CSV)
            @RequestParam(required = false, name="gradeLevels") String gradeLevelsCsv, // "5,4,3"
            @RequestParam(required = false, name="grades")      String gradesCsv,      // legacy "5,4,3"

            // 별점 밴드/어메니티 (CSV)
            @RequestParam(required = false, name="ratingBands") String ratingBandsCsv, // "1,3"
            @RequestParam(required = false, name="amenityIds")  String amenityIdsCsv,  // "2,8,9"

            // 레거시 지역/인원
            @RequestParam(required = false) String region,   // -> q 대체
            @RequestParam(required = false) Integer guests,  // -> adults 대체

            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "0")  Integer offset
    ) {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }

        // q 하위호환
        final String effQ = (q != null && !q.isBlank()) ? q.trim()
                : (region != null && !region.isBlank() ? region.trim() : "");
        final int effAdults   = (adults   != null ? adults   : (guests != null ? guests : 1));
        final int effChildren = (children != null ? children : 0);

        // ✅ 가격 이름 정규화 (priceMin/priceMax 우선, 없으면 minPrice/maxPrice 사용)
        final Long effMinPrice = (priceMin != null ? priceMin : minPrice);
        final Long effMaxPrice = (priceMax != null ? priceMax : maxPrice);

        // ✅ 성급: grades + gradeLevels 병합(합집합)
        List<Integer> gradeLevels =
                mergeIntsCsv(gradesCsv, gradeLevelsCsv); // 둘 중 오는 값 모두 반영

        // 별점밴드/어메니티
        List<Integer> ratingBands = parseCsvToInts(ratingBandsCsv);
        List<Long>    amenityIds  = parseCsvToLongs(amenityIdsCsv);

        return service.searchV3(
                checkIn, checkOut, effQ, effAdults, effChildren,
                effMinPrice, effMaxPrice,
                gradeLevels, ratingBands, amenityIds,
                sort,
                limit, offset
        );
    }

    /** 호텔 자동완성 */
    @GetMapping("/suggest")
    public List<HotelSuggestItem> suggest(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "8") int limit
    ) {
        return service.suggest(q, limit);
    }

    // ------- helpers -------
    private static List<Integer> parseCsvToInts(String s){
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(","))
                .map(String::trim).filter(v -> !v.isEmpty())
                .map(Integer::valueOf).collect(Collectors.toList());
    }
    private static List<Long> parseCsvToLongs(String s){
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(","))
                .map(String::trim).filter(v -> !v.isEmpty())
                .map(Long::valueOf).collect(Collectors.toList());
    }
    /** gradesCsv + gradeLevelsCsv 합치고 중복 제거 */
    private static List<Integer> mergeIntsCsv(String... parts){
        return Arrays.stream(parts)
                .filter(p -> p != null && !p.isBlank())
                .flatMap(p -> Arrays.stream(p.split(",")))
                .map(String::trim).filter(v -> !v.isEmpty())
                .map(Integer::valueOf)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
