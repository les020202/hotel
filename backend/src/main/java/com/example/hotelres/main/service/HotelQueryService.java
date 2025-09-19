package com.example.hotelres.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.example.hotelres.main.model.Hotel; // DTO
import com.example.hotelres.main.repo.HotelJpaRepository;
import com.example.hotelres.main.repo.HotelSpecifications;

@Service
@RequiredArgsConstructor
public class HotelQueryService {

    private final HotelJpaRepository repo;

    public Page<Hotel> search(
            String q,
            String region,
            boolean regionExact,
            Integer gradeMin,
            Boolean hasHomepage,
            Pageable pageable
    ) {
        var spec = HotelSpecifications.filter(q, region, regionExact, gradeMin, hasHomepage);
        Page<com.example.hotelres.hotel.Hotel> page = repo.findAll(spec, pageable);
        return page.map(this::toModel);
    }

    public Hotel getOne(Long id) {
        return repo.findById(id).map(this::toModel)
                .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다: " + id));
    }

    // ⬇️ 엔티티 -> DTO 변환 (파라미터 타입을 엔티티로!)
    private Hotel toModel(com.example.hotelres.hotel.Hotel e) {
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

    // ⬇️ BigDecimal/Double 모두 처리 가능하도록 일반화
    private Double toDouble(Number n) {
        return n == null ? null : n.doubleValue();
    }
}
