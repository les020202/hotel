package com.example.hotelres.main.service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.*; 
import java.util.concurrent.ThreadLocalRandom; 

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.example.hotelres.main.entity.BookingDayEntity.Status;
import com.example.hotelres.main.model.Hotel;
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
        Page<com.example.hotelres.admin.hotel.Hotel> page = repo.findAll(spec, pageable);
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

    private Double toDouble(Number n) { return n == null ? null : n.doubleValue(); }

    /** 호텔별 최소 가격 조회 */
    public Map<Long, Integer> getMinPricesByHotelIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        var rows = bookingDayQueryRepository.findMinPriceByHotelIds(ids, LocalDate.now(), Status.OPEN);

        Map<Long,Integer> out = new HashMap<>();
        for (Object[] r : rows) {
            Long hotelId  = ((Number) r[0]).longValue();
            Integer price = ((Number) r[1]).intValue();
            out.put(hotelId, price);
        }
        return out;
    }

    // -------------------- 여기부터 추천 수정분 --------------------

    /** 추천 호텔 목록(id/minPrice) - 중복 제거 + 매요청 랜덤 셔플 */
    public List<Map<String,Object>> getRecommended(int limit) {
        var rows = bookingDayQueryRepository.findCheapestHotelIds(LocalDate.now(), Status.OPEN);

        List<Map<String,Object>> all = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("hotelId", ((Number) r[0]).longValue());
            m.put("minPrice", ((Number) r[1]).intValue());
            all.add(m);
        }

        List<Map<String,Object>> uniq = dedupe(all, m -> m.get("hotelId"));

        // ✅ 매요청 랜덤 셔플 (새로고침마다 순서 변함)
        shuffleEachRequest(uniq);

        int end = Math.min(limit, uniq.size());
        return uniq.subList(0, end);
    }

    /** 추천 카드(id, name, address, coverImageUrl, minPrice) - 매요청 랜덤 셔플 */
    public List<Map<String,Object>> getRecommendedCards(int limit) {
        var rows = bookingDayQueryRepository.findRecommendedHotelCardRows(LocalDate.now(), Status.OPEN);

        List<Map<String,Object>> all = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id",            ((Number) r[0]).longValue());
            m.put("name",          (String) r[1]);
            m.put("address",       (String) r[2]);
            m.put("coverImageUrl", (String) r[3]);
            m.put("minPrice",      r[4] == null ? null : ((Number) r[4]).intValue());
            all.add(m);
        }

        List<Map<String,Object>> uniq = dedupe(all, m -> m.get("id"));

        // (선택) 지역 편중 완화는 유지
        List<Map<String,Object>> diversified = diversifyByArea(uniq, 2);
        if (diversified.size() < uniq.size()) {
            Set<Object> kept = new HashSet<>();
            for (var m : diversified) kept.add(m.get("id"));
            for (var m : uniq) if (kept.add(m.get("id"))) diversified.add(m);
        }

        // ✅ 매요청 랜덤 셔플
        shuffleEachRequest(diversified);

        int end = Math.min(limit, diversified.size());
        return diversified.subList(0, end);
    }

// -------------------- 유틸 --------------------

// (기존) dedupe/diversifyByArea 그대로 유지

/** ✅ 매요청마다 랜덤 셔플 */
private <T> void shuffleEachRequest(List<T> list) {
    if (list == null || list.size() <= 1) return;
    Collections.shuffle(list, ThreadLocalRandom.current());
}

    // -------------------- 유틸 --------------------

    private <T> List<T> dedupe(List<T> src, java.util.function.Function<T, ?> keyFn) {
        Set<Object> seen = new LinkedHashSet<>();
        List<T> out = new ArrayList<>(src.size());
        for (T t : src) {
            Object k = keyFn.apply(t);
            if (seen.add(k)) out.add(t);
        }
        return out;
    }

    private long dailySeed() {
        return LocalDate.now().toEpochDay(); // 필요 시 ZoneId.of("Asia/Seoul") 고려
    }

    private <T> void shuffleWithSeed(List<T> list, long seed) {
        if (list.size() <= 1) return;
        long s = (seed ^ 0x9E3779B97F4A7C15L);
        for (int i = list.size() - 1; i > 0; i--) {
            s = (s * 1664525L + 1013904223L) & 0xFFFFFFFFL;
            int j = (int)(s % (i + 1));
            Collections.swap(list, i, j);
        }
    }

    private List<Map<String, Object>> diversifyByArea(List<Map<String, Object>> src, int maxPerArea) {
        Map<String, Integer> cap = new HashMap<>();
        List<Map<String, Object>> out = new ArrayList<>(src.size());
        for (Map<String, Object> m : src) {
            String addr = Objects.toString(m.get("address"), "");
            String area = addr;
            String[] toks = addr.split("\\s+");
            if (toks.length >= 2) area = toks[0] + " " + toks[1];
            int cnt = cap.getOrDefault(area, 0);
            if (cnt < maxPerArea) {
                out.add(m);
                cap.put(area, cnt + 1);
            }
        }
        return out;
    }
}
