// src/main/java/com/example/hotelres/search/SearchService.java
package com.example.hotelres.search;

import com.example.hotelres.common.dto.PagedResponse;
import com.example.hotelres.search.dto.HotelCardDto;
import com.example.hotelres.search.dto.HotelSuggestItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository repo;

    /** 공백 제거 유틸 */
    private static String norm(String s){
        return s == null ? "" : s.replaceAll("\\s+", "").trim();
    }

    /** 국내 지역 키(공백 무시 정규형 비교) */
    private static final Set<String> REGION_KEYS_NORM = Set.of(
            "서울","인천","울산","경상북도","경상남도","부산","전라남도","전라북도",
            "강원","경기","충청북도","충청남도","광주","대구","대전"
    );

    /** 허용 정렬 키 */
    private static final Set<String> ALLOWED_SORT = Set.of(
            "rating",        // 기본: 평점 desc, tie -> 리뷰많은순 desc -> id asc
            "price_asc",
            "price_desc",
            "grade_desc",
            "reviews_desc"
    );

    /** sort 정규화(허용 외 입력은 rating으로 강등) */
    private static String normalizeSort(String s){
        String v = (s == null ? "rating" : s.trim().toLowerCase());
        return ALLOWED_SORT.contains(v) ? v : "rating";
    }

    @Transactional(readOnly = true)
    public PagedResponse<HotelCardDto> searchV3(
            LocalDate checkIn,
            LocalDate checkOut,
            String q,
            Integer adults,
            Integer children,
            Long priceMin,
            Long priceMax,
            List<Integer> gradeLevels,   // 성급 다중
            List<Integer> ratingBands,   // [1|2|3|4] 다중
            List<Long> amenityIds,       // 어메니티 ID 다중(모두 포함)
            String sort,                 // ✅ 정렬 키 추가
            Integer limit,
            Integer offset
    ) {
        // 기본값 보정
        int a = (adults == null || adults < 1) ? 1 : adults;
        int c = (children == null || children < 0) ? 0 : children;
        int pageSize = (limit == null || limit <= 0) ? 10 : limit;
        int off = (offset == null || offset < 0) ? 0 : offset;

        final String qTrim = (q == null) ? "" : q.trim();
        final boolean regionOnly = REGION_KEYS_NORM.contains(norm(qTrim));
        final String sortKey = normalizeSort(sort); // ✅ 정렬 정규화

        // ✅ 가격 정규화: 둘 다 있어야 의미 있음, min>max면 스왑
        Long min = priceMin, max = priceMax;
        if (min == null || max == null) {
            min = null; max = null; // 한쪽만 오면 가격필터 미적용
        } else if (min > max) {
            long t = min; min = max; max = t;
        }

        // ✅ 리스트 정규화: null → 빈목록, 중복 제거 + 정렬
        List<Integer> gradesNorm = normalizeInts(gradeLevels);
        List<Integer> bandsNorm  = normalizeInts(ratingBands);
        List<Long>    amensNorm  = normalizeLongs(amenityIds);

        // 목록 조회
        List<HotelCardDto> items = repo.findHotelsV3(
                checkIn, checkOut, qTrim, a, c,
                min, max, gradesNorm, bandsNorm, amensNorm,
                regionOnly, sort, pageSize, off   // ✅ sort 전달
        );

        // 총계 조회(정렬 불필요)
        long total = repo.countHotelsV3(
                checkIn, checkOut, qTrim, a, c,
                min, max, gradesNorm, bandsNorm, amensNorm,
                regionOnly
        );

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

    /** 레거시(region/guests) 호출용 래퍼 */
    @Transactional(readOnly = true)
    public PagedResponse<HotelCardDto> searchLegacy(
            LocalDate checkIn,
            LocalDate checkOut,
            String region,      // -> q
            Integer guests,     // -> adults
            Integer limit,
            Integer offset
    ) {
        return searchV3(
                checkIn, checkOut,
                region,                 // q
                guests, 0,              // adults, children
                null, null,             // priceMin, priceMax
                List.of(),              // gradeLevels
                List.of(),              // ratingBands
                List.of(),              // amenityIds
                "rating",               // ✅ 기본 정렬
                limit, offset
        );
    }

    /** 호텔 자동완성 */
    @Transactional(readOnly = true)
    public List<HotelSuggestItem> suggest(String q, int limit) {
        int lim = (limit <= 0) ? 8 : limit;
        return repo.suggestHotels(q, lim);
    }

    // ---- helpers ----
    private static List<Integer> normalizeInts(List<Integer> src){
        if (src == null) return List.of();
        return src.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    private static List<Long> normalizeLongs(List<Long> src){
        if (src == null) return List.of();
        return src.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
