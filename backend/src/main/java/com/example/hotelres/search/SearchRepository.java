// src/main/java/com/example/hotelres/search/SearchRepository.java
package com.example.hotelres.search;

import com.example.hotelres.search.dto.HotelCardDto;
import com.example.hotelres.search.dto.HotelSuggestItem;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SearchRepository {

    @PersistenceContext
    private final EntityManager em;

    // 리뷰 테이블/컬럼 (스키마에 맞춰 필요하면 바꾸세요)
    private static final String REVIEW_TABLE    = "reviews";
    private static final String REVIEW_ID_COL   = "id";
    private static final String REVIEW_HOTEL_FK = "hotel_id";
    // visible=1 만 집계하려면 아래 상수를 true 로 바꿔 조인 조건에 붙입니다.
    private static final boolean COUNT_ONLY_VISIBLE = false;

    // 객실가 집계 서브쿼리 (그대로)
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
          AND rt.capacity >= (:adults + GREATEST(:children - 2, 0))
        GROUP BY bd.hotel_id, bd.room_type_id
        HAVING COUNT(*) = DATEDIFF(:checkOut, :checkIn)
           AND MIN(bd.remaining_qty) > 0
    """;

    /* =========================
       V3: 목록 조회 (+ 정렬)
       ========================= */
    public List<HotelCardDto> findHotelsV3(
            LocalDate checkIn, LocalDate checkOut, String q,
            int adults, int children,
            Long priceMin, Long priceMax,
            List<Integer> gradeLevels,
            List<Integer> ratingBands,
            List<Long> amenityIds,
            boolean regionOnly,
            String sort,
            int limit, int offset
    ) {
        boolean useGrades = gradeLevels != null && !gradeLevels.isEmpty();
        boolean useBands  = ratingBands != null && !ratingBands.isEmpty();
        boolean useAmen   = amenityIds  != null && !amenityIds.isEmpty();
        boolean usePrice  = (priceMin != null) || (priceMax != null);
        boolean useQ      = q != null && !q.isBlank();

        String visibleCond = COUNT_ONLY_VISIBLE ? " AND rv.visible = 1 " : "";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append(" h.id AS hotel_id,")
                .append(" h.name,")
                .append(" h.address,")
                .append(" h.cover_image_url,")
                .append(" h.rating,")
                .append(" h.grade_level,")
                // ✅ 리뷰 수를 COUNT(rv.id) 로 계산하고 별칭을 reviews_cnt 로 통일
                .append(" COUNT(rv.").append(REVIEW_ID_COL).append(") AS reviews_cnt,")
                .append(" MIN(t.price_sum) AS starting_from ")
                .append("FROM (").append(INNER_ROOMTYPE_SQL).append(") t ")
                .append("JOIN hotels h ON h.id = t.hotel_id ");

        if (useAmen) sql.append("JOIN hotel_amenities ha ON ha.hotel_id = h.id ");

        // ✅ 리뷰 LEFT JOIN (0개여도 0으로 집계)
        sql.append("LEFT JOIN ").append(REVIEW_TABLE).append(" rv ")
                .append(" ON rv.").append(REVIEW_HOTEL_FK).append(" = h.id")
                .append(visibleCond);

        sql.append(" WHERE 1=1 ");

        if (useQ) {
            if (regionOnly) {
                sql.append("AND ( h.region LIKE CONCAT(:regionKey,'%') OR h.address LIKE CONCAT('%',:regionKey,'%') ) ");
            } else {
                sql.append("AND ( h.name LIKE CONCAT('%',:q,'%') OR h.region LIKE CONCAT(:q,'%') ) ");
            }
        }
        if (useGrades) sql.append("AND h.grade_level IN (:grades) ");

        if (useBands) {
            List<String> ors = new ArrayList<>();
            if (ratingBands.contains(1)) ors.add("(h.rating >= 1.0 AND h.rating < 2.0)");
            if (ratingBands.contains(2)) ors.add("(h.rating >= 2.0 AND h.rating < 3.0)");
            if (ratingBands.contains(3)) ors.add("(h.rating >= 3.0 AND h.rating < 4.0)");
            if (ratingBands.contains(4)) ors.add("(h.rating >= 4.0 AND h.rating <= 5.0)");
            if (!ors.isEmpty()) sql.append("AND (").append(String.join(" OR ", ors)).append(") ");
        }

        // ✅ ONLY_FULL_GROUP_BY 대응: SELECT의 비집계 컬럼 + COUNT(rv.id)만 집계
        sql.append("GROUP BY h.id, h.name, h.address, h.cover_image_url, h.rating, h.grade_level ");

        // HAVING (어메니티 all-of + 가격)
        List<String> havingConds = new ArrayList<>();
        if (useAmen) {
            havingConds.add("COUNT(DISTINCT CASE WHEN ha.amenity_id IN (:amenIds) THEN ha.amenity_id END) = :amenCnt");
        }
        if (usePrice) {
            if (priceMin != null) havingConds.add("MIN(t.price_sum) >= :priceMin");
            if (priceMax != null) havingConds.add("MIN(t.price_sum) <= :priceMax");
        }
        if (!havingConds.isEmpty()) {
            sql.append(" HAVING ").append(String.join(" AND ", havingConds)).append(" ");
        }

        // ✅ ORDER BY (reviews_cnt 별칭 사용)
        String key = (sort == null || sort.isBlank()) ? "rating" : sort;
        String orderBy;
        switch (key) {
            case "price_asc":
                orderBy = " starting_from ASC, h.grade_level DESC, h.name ASC ";
                break;
            case "price_desc":
                orderBy = " starting_from DESC, h.grade_level DESC, h.name ASC ";
                break;
            case "grade_desc":
                orderBy = " h.grade_level DESC, starting_from ASC, h.name ASC ";
                break;
            case "reviews_desc":
                orderBy = " reviews_cnt DESC, h.rating DESC, h.id ASC ";
                break;
            case "rating":
            default:
                // 기본: 평점 ↓, 리뷰수 ↓, id ↑
                orderBy = " h.rating DESC, reviews_cnt DESC, h.id ASC ";
                break;
        }
        sql.append("ORDER BY ").append(orderBy)
                .append("LIMIT :limit OFFSET :offset");

        Query qy = em.createNativeQuery(sql.toString())
                .setParameter("checkIn", java.sql.Date.valueOf(checkIn))
                .setParameter("checkOut", java.sql.Date.valueOf(checkOut))
                .setParameter("adults", adults)
                .setParameter("children", children)
                .setParameter("limit", limit)
                .setParameter("offset", offset);

        if (useQ) {
            if (regionOnly) qy.setParameter("regionKey", q.trim());
            else            qy.setParameter("q", q.trim());
        }
        if (useGrades) qy.setParameter("grades", gradeLevels);
        if (useAmen) {
            qy.setParameter("amenIds", amenityIds);
            qy.setParameter("amenCnt", amenityIds.size());
        }
        if (priceMin != null) qy.setParameter("priceMin", priceMin);
        if (priceMax != null) qy.setParameter("priceMax", priceMax);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = qy.getResultList();

        List<HotelCardDto> list = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Long    hotelId      = ((Number) r[0]).longValue();
            String  name         = (String)  r[1];
            String  address      = (String)  r[2];
            String  cover        = (String)  r[3];
            Double  rating       = (r[4] == null) ? null : ((Number) r[4]).doubleValue();
            Integer gradeLevel   = (r[5] == null) ? null : ((Number) r[5]).intValue();
            // r[6] = reviews_cnt (원하면 DTO에 필드 추가해서 사용)
            Long    startingFrom = (r[7] == null) ? null : ((Number) r[7]).longValue();

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

    /* =========================
       V3: 총계 (정렬 불필요) — 변경 없음
       ========================= */
    public long countHotelsV3(
            LocalDate checkIn, LocalDate checkOut, String q,
            int adults, int children,
            Long priceMin, Long priceMax,
            List<Integer> gradeLevels,
            List<Integer> ratingBands,
            List<Long> amenityIds,
            boolean regionOnly
    ) {
        boolean useGrades = gradeLevels != null && !gradeLevels.isEmpty();
        boolean useBands  = ratingBands != null && !ratingBands.isEmpty();
        boolean useAmen   = amenityIds  != null && !amenityIds.isEmpty();
        boolean usePrice  = (priceMin != null) || (priceMax != null);
        boolean useQ      = q != null && !q.isBlank();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ( ");
        sql.append("""
            SELECT h.id
            FROM (""").append(INNER_ROOMTYPE_SQL).append(") t ")
                .append("JOIN hotels h ON h.id = t.hotel_id ");

        if (useAmen) sql.append("JOIN hotel_amenities ha ON ha.hotel_id = h.id ");

        sql.append("WHERE 1=1 ");

        if (useQ) {
            if (regionOnly) {
                sql.append("AND ( h.region LIKE CONCAT(:regionKey,'%%') OR h.address LIKE CONCAT('%%',:regionKey,'%%') ) ");
            } else {
                sql.append("AND ( h.name LIKE CONCAT('%%',:q,'%%') OR h.region LIKE CONCAT(:q,'%%') ) ");
            }
        }
        if (useGrades) sql.append("AND h.grade_level IN (:grades) ");

        if (useBands) {
            List<String> ors = new ArrayList<>();
            if (ratingBands.contains(1)) ors.add("(h.rating >= 1.0 AND h.rating < 2.0)");
            if (ratingBands.contains(2)) ors.add("(h.rating >= 2.0 AND h.rating < 3.0)");
            if (ratingBands.contains(3)) ors.add("(h.rating >= 3.0 AND h.rating < 4.0)");
            if (ratingBands.contains(4)) ors.add("(h.rating >= 4.0 AND h.rating <= 5.0)");
            if (!ors.isEmpty()) sql.append("AND (").append(String.join(" OR ", ors)).append(") ");
        }

        sql.append("GROUP BY h.id ");

        List<String> havingConds = new ArrayList<>();
        if (useAmen) {
            havingConds.add("COUNT(DISTINCT CASE WHEN ha.amenity_id IN (:amenIds) THEN ha.amenity_id END) = :amenCnt");
        }
        if (usePrice) {
            if (priceMin != null) havingConds.add("MIN(t.price_sum) >= :priceMin");
            if (priceMax != null) havingConds.add("MIN(t.price_sum) <= :priceMax");
        }
        if (!havingConds.isEmpty()) {
            sql.append(" HAVING ").append(String.join(" AND ", havingConds)).append(" ");
        }

        sql.append(") x");

        Query qy = em.createNativeQuery(sql.toString())
                .setParameter("checkIn", java.sql.Date.valueOf(checkIn))
                .setParameter("checkOut", java.sql.Date.valueOf(checkOut))
                .setParameter("adults", adults)
                .setParameter("children", children);

        if (useQ) {
            if (regionOnly) qy.setParameter("regionKey", q.trim());
            else            qy.setParameter("q", q.trim());
        }
        if (useGrades) qy.setParameter("grades", gradeLevels);
        if (useAmen) {
            qy.setParameter("amenIds", amenityIds);
            qy.setParameter("amenCnt", amenityIds.size());
        }
        if (priceMin != null) qy.setParameter("priceMin", priceMin);
        if (priceMax != null) qy.setParameter("priceMax", priceMax);

        Object v = qy.getSingleResult();
        return ((Number) v).longValue();
    }

    // ---- 자동완성 (기존 유지) ----
    private static final String SUGGEST_SQL = """
        SELECT
            h.id              AS hotel_id,
            h.name            AS name,
            h.region          AS region,
            h.address         AS address,
            h.cover_image_url AS cover_image_url
        FROM hotels h
        WHERE (:q IS NOT NULL AND :q <> '')
          AND h.name LIKE CONCAT('%%', :q, '%%')
        ORDER BY h.name ASC
        LIMIT :limit
    """;

    public List<HotelSuggestItem> suggestHotels(String q, int limit) {
        Query query = em.createNativeQuery(SUGGEST_SQL)
                .setParameter("q", q)
                .setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<HotelSuggestItem> list = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Long   hotelId = ((Number) r[0]).longValue();
            String name    = (String) r[1];
            String region  = (String) r[2];
            String address = (String) r[3];
            String cover   = (String) r[4];

            list.add(HotelSuggestItem.builder()
                    .hotelId(hotelId)
                    .name(name)
                    .region(region)
                    .address(address)
                    .coverImageUrl(cover)
                    .build());
        }
        return list;
    }
}
