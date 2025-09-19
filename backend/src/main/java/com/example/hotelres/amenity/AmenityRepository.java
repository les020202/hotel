// src/main/java/com/example/hotelres/amenity/AmenityRepository.java
package com.example.hotelres.amenity;

import com.example.hotelres.amenity.dto.AmenityDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AmenityRepository {
    @PersistenceContext
    private final EntityManager em;

    public List<AmenityDto> list(String scope) {
        String sql = """
            SELECT id, code, name, sort_order
            FROM amenities
            WHERE (:scope IS NULL OR scope = :scope)
            ORDER BY sort_order, id
        """;

        var q = em.createNativeQuery(sql).setParameter("scope", scope);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();  // ✅ 타입 고정

        List<AmenityDto> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Long id = ((Number) r[0]).longValue();
            String code = (String) r[1];
            String name = (String) r[2];
            Integer sortOrder = (r[3] == null) ? 0 : ((Number) r[3]).intValue();
            out.add(new AmenityDto(id, code, name, sortOrder));
        }
        return out;
    }
}
