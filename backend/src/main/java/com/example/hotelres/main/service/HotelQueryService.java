package com.example.hotelres.main.service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.*;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.example.hotelres.main.entity.BookingDayEntity.Status;
import com.example.hotelres.main.model.Hotel; // ✅ DTO만 import
import com.example.hotelres.main.repo.BookingDayQueryRepository;
import com.example.hotelres.main.repo.HotelJpaRepository;
import com.example.hotelres.main.repo.HotelSpecifications;

@Service
@RequiredArgsConstructor
public class HotelQueryService {

    private final HotelJpaRepository repo;
    private final BookingDayQueryRepository bookingDayQueryRepository;

    /** 호텔 검색 */
    public Page<Hotel> search(
            String q,
            String region,
            boolean regionExact,
            Integer gradeMin,
            Boolean hasHomepage,
            Pageable pageable
    ) {
        var spec = HotelSpecifications.filter(q, region, regionExact, gradeMin, hasHomepage);

        // 엔티티 Page (풀 패키지로 명시)
        Page<com.example.hotelres.admin.hotel.Hotel> page = repo.findAll(spec, pageable);

        // DTO 변환
        return page.map(this::toModel);
    }

    /** 단건 조회 */
    public Hotel getOne(Long id) {
        return repo.findById(id)
                .map(this::toModel)
                .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다: " + id));
    }

    /** 엔티티 → DTO 변환 */
    private Hotel toModel(com.example.hotelres.admin.hotel.Hotel e) {
        return Hotel.builder()
                .id(e.getId())
                .name(e.getName())
                .rating(toDouble(e.getRating()))
                .officialGrade(e.getOfficialGrade())
                .gradeLevel(e.getGradeLevel())
                .region(e.getRegion())
                .address(e.getAddress())
                .phone(e.getPhone())
                .homepageUrl(e.getHomepageUrl())
                .latitude(toDouble(e.getLatitude()))
                .longitude(toDouble(e.getLongitude()))
                .canonicalKey(e.getCanonicalKey())
                .coverImageType(e.getCoverImageType() == null ? null : e.getCoverImageType().name())
                .coverImageUrl(e.getCoverImageUrl())
                .coverImageTemplate(e.getCoverImageTemplate() == null ? null : e.getCoverImageTemplate().name())
                .build();
    }

    /** BigDecimal → Double 변환 */
    private Double toDouble(Number n) {
        return n == null ? null : n.doubleValue();
    }

    /** 호텔별 최소 가격 조회 */
    public Map<Long, Integer> getMinPricesByHotelIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        var rows = bookingDayQueryRepository.findMinPriceByHotelIds(ids, LocalDate.now(), Status.OPEN);

        Map<Long,Integer> out = new HashMap<>();
        for (Object[] r : rows) {
            Long hotelId  = ((Number) r[0]).longValue();   // ✅ 안전 캐스팅
            Integer price = ((Number) r[1]).intValue();    // ✅ BigDecimal/Long 등 커버
            out.put(hotelId, price);
        }
        return out;
    }

    /** 추천 호텔 목록(id/minPrice) */
    public List<Map<String,Object>> getRecommended(int limit) {
        var rows = bookingDayQueryRepository.findCheapestHotelIds(LocalDate.now(), Status.OPEN);
        List<Map<String,Object>> resp = new ArrayList<>();
        for (int i=0; i<rows.size() && i<limit; i++) {
            Object[] r = rows.get(i);
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("hotelId", ((Number) r[0]).longValue()); // ✅ 안전 캐스팅
            m.put("minPrice", ((Number) r[1]).intValue()); // ✅ 안전 캐스팅
            resp.add(m);
        }
        return resp;
    }
    
    /** ⬇️ 추가: 추천 카드(id, name, address, coverImageUrl, minPrice) */
    public List<Map<String,Object>> getRecommendedCards(int limit) {
        var rows = bookingDayQueryRepository.findRecommendedHotelCardRows(LocalDate.now(), Status.OPEN);
        List<Map<String,Object>> resp = new ArrayList<>();
        for (int i = 0; i < rows.size() && i < limit; i++) {
            Object[] r = rows.get(i);
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id",            ((Number) r[0]).longValue());
            m.put("name",          (String) r[1]);
            m.put("address",       (String) r[2]);
            m.put("coverImageUrl", (String) r[3]);
            m.put("minPrice",      r[4] == null ? null : ((Number) r[4]).intValue());
            resp.add(m);
        }
        return resp;
    }
}
