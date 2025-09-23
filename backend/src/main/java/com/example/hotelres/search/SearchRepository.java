// src/main/java/com/example/hotelres/search/SearchRepository.java
package com.example.hotelres.search;

import com.example.hotelres.search.dto.HotelCardDto;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.sql.Date;

@Repository
@RequiredArgsConstructor
public class SearchRepository {

    @PersistenceContext
    private final EntityManager em;

    // (1) 내부 서브쿼리는 그대로 (guests 조건 이미 잘 들어가 있음)
    private static final String INNER_ROOMTYPE_SQL = """
    SELECT
        bd.hotel_id      AS hotel_id,
        bd.room_type_id  AS room_type_id,
        SUM(bd.price)    AS price_sum
    FROM booking_day bd
    JOIN room_types rt ON rt.id = bd.room_type_id
    WHERE bd.stay_date >= :checkIn
      AND bd.stay_date <  :checkOut
      AND bd.is_sellable = 1
      AND rt.capacity >= :guests
    GROUP BY bd.hotel_id, bd.room_type_id
    HAVING COUNT(*) = DATEDIFF(:checkOut, :checkIn)
       AND MIN(bd.remaining_qty) > 0
""";

    // (2) 목록용: = 를 LIKE 접두사로, 그리고 % -> %% 로 이스케이프
    private static final String LIST_SQL = """
    SELECT
        h.id               AS hotel_id,
        h.name,
        h.address,
        h.cover_image_url,
        h.rating,
        h.grade_level,
        MIN(t.price_sum)   AS starting_from
    FROM ( %s ) t
    JOIN hotels h ON h.id = t.hotel_id
    WHERE (:region IS NULL OR h.region LIKE CONCAT(:region, '%%'))   -- ★ 여기
    GROUP BY h.id, h.name, h.address, h.cover_image_url, h.rating, h.grade_level
    ORDER BY starting_from ASC, h.grade_level DESC, h.name ASC
    LIMIT :limit OFFSET :offset
""";

    // (3) 카운트용도 동일하게
    private static final String COUNT_SQL = """
    SELECT COUNT(*) FROM (
        SELECT h.id
        FROM ( %s ) t
        JOIN hotels h ON h.id = t.hotel_id
        WHERE (:region IS NULL OR h.region LIKE CONCAT(:region, '%%'))  -- ★ 여기
        GROUP BY h.id
    ) x
""";


    // ✅ guests 파라미터 추가
    public List<HotelCardDto> findHotels(LocalDate checkIn,
                                         LocalDate checkOut,
                                         String region,
                                         int guests,
                                         int limit,
                                         int offset) {
        String sql = LIST_SQL.formatted(INNER_ROOMTYPE_SQL);
        Query q = em.createNativeQuery(sql)
                .setParameter("checkIn", Date.valueOf(checkIn))
                .setParameter("checkOut", Date.valueOf(checkOut))
                .setParameter("region", region)
                .setParameter("guests", guests)              // ✅ 전달
                .setParameter("limit", limit)
                .setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        List<HotelCardDto> list = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Long    hotelId      = ((Number) r[0]).longValue();
            String  name         = (String)  r[1];
            String  address      = (String)  r[2];
            String  cover        = (String)  r[3];
            Double  rating       = (r[4] == null) ? null : ((Number) r[4]).doubleValue();
            Integer gradeLevel   = (r[5] == null) ? null : ((Number) r[5]).intValue();
            Long    startingFrom = (r[6] == null) ? null : ((Number) r[6]).longValue();

            list.add(HotelCardDto.builder()
                    .hotelId(hotelId)
                    .name(name)
                    .address(address)
                    .coverImageUrl(cover)
                    .rating(rating)
                    .gradeLevel(gradeLevel)
                    .startingFrom(startingFrom)
                    .imagesCount(12)
                    .amenitiesCount(null)
                    .build());
        }
        return list;
    }

    // ✅ guests 파라미터 추가
    public long countHotels(LocalDate checkIn,
                            LocalDate checkOut,
                            String region,
                            int guests) {
        String sql = COUNT_SQL.formatted(INNER_ROOMTYPE_SQL);
        Query q = em.createNativeQuery(sql)
                .setParameter("checkIn", Date.valueOf(checkIn))
                .setParameter("checkOut", Date.valueOf(checkOut))
                .setParameter("region", region)
                .setParameter("guests", guests);            // ✅ 전달

        Object v = q.getSingleResult();
        return ((Number) v).longValue();
    }
}
