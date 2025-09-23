// src/main/java/com/example/demo/repo/HotelSpecifications.java
package com.example.hotelres.main.repo;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.example.hotelres.hotel.Hotel;

import java.util.ArrayList;
import java.util.List;

public final class HotelSpecifications {

    private HotelSpecifications() { }

    public static Specification<Hotel> filter(
            String q,
            String region,
            Boolean regionExact,
            Integer gradeMin,
            Boolean hasHomepage
    ) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            // ✅ 키워드: name / address 부분일치
            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase() + "%";
                Expression<String> nameExp = cb.lower(root.get("name"));
                Expression<String> addrExp = cb.lower(root.get("address"));
                ps.add(cb.or(cb.like(nameExp, like), cb.like(addrExp, like)));
            }

            // 지역 필터
            if (StringUtils.hasText(region)) {
                if (Boolean.TRUE.equals(regionExact)) {
                    ps.add(cb.equal(root.get("region"), region));
                } else {
                    ps.add(cb.like(cb.lower(root.get("region")), "%" + region.toLowerCase() + "%"));
                }
            }

            // 최소 등급(grade_level)
            if (gradeMin != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("gradeLevel"), gradeMin));
            }

            // 홈페이지 존재 여부
            if (hasHomepage != null) {
                if (hasHomepage) {
                    ps.add(cb.isNotNull(root.get("homepageUrl")));
                } else {
                    ps.add(cb.isNull(root.get("homepageUrl")));
                }
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
