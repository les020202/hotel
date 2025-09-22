// src/main/java/com/example/hotelres/hotel/HotelDetailsRepository.java
package com.example.hotelres.hotel;

import com.example.hotelres.hotel.dto.HotelDetailsDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class HotelDetailsRepository {

    @PersistenceContext
    private EntityManager em;

    /* ---------- number helpers ---------- */
    private static Long nLong(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd.longValue();
        return ((Number) v).longValue();
    }
    private static Integer nInt(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd.intValue();
        return ((Number) v).intValue();
    }
    private static Double nDouble(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd.doubleValue();
        return ((Number) v).doubleValue();
    }

    /* ---------- SQLs ---------- */

    // 호텔 기본
    private static final String HOTEL_SQL = """
        SELECT h.id, h.name, h.region, h.address, h.rating, h.grade_level, h.cover_image_url
        FROM hotels h
        WHERE h.id = :id
    """;

    // 룸타입 오퍼(템플릿 이미지 포함) — ✅ 인원 조건(capacity) 추가
    private static final String ROOMTYPE_SQL = """
        SELECT
            rt.id                   AS room_type_id,
            rt.name                 AS room_type_name,
            rt.capacity             AS capacity,
            rt.area_sqm             AS area_sqm,
            MIN(bd.remaining_qty)   AS min_remaining,
            SUM(bd.price)           AS price_sum,
            DATEDIFF(:co, :ci)      AS nights,
            tpl.image_url           AS template_image_url
        FROM booking_day bd
        JOIN room_types rt
          ON rt.id = bd.room_type_id
        LEFT JOIN room_type_templates tpl
          ON tpl.code = rt.type_code
        WHERE bd.hotel_id   = :hid
          AND bd.stay_date >= :ci
          AND bd.stay_date <  :co
          AND bd.is_sellable = 1
          AND rt.capacity >= :guests        -- ✅ 여기 추가
        GROUP BY rt.id, rt.name, rt.capacity, rt.area_sqm, tpl.image_url
        HAVING COUNT(*) = DATEDIFF(:co, :ci)
           AND MIN(bd.remaining_qty) > 0
        ORDER BY price_sum ASC, rt.capacity DESC, rt.name ASC
    """;

    // 갤러리(공용 12장)
    private static final String DEFAULT_ROOM_PHOTOS_SQL = """
        SELECT url, alt_text, sort_order
        FROM default_room_photos
        ORDER BY sort_order ASC, id ASC
    """;

    /* ---------- main ---------- */
    // ✅ guests 파라미터 추가
    public HotelDetailsDto findDetails(Long hotelId, LocalDate ci, LocalDate co, int guests) {
        // 1) 호텔 기본
        List<?> hotelRows = em.createNativeQuery(HOTEL_SQL)
                .setParameter("id", hotelId)
                .getResultList();
        if (hotelRows.isEmpty()) return null;

        Object[] hrow = (Object[]) hotelRows.get(0);

        HotelDetailsDto.Hotel h = new HotelDetailsDto.Hotel();
        h.setId(nLong(hrow[0]));
        h.setName((String) hrow[1]);
        h.setRegion((String) hrow[2]);
        h.setAddress((String) hrow[3]);
        h.setRating(nDouble(hrow[4]));
        h.setGradeLevel(nInt(hrow[5]));
        String cover = (String) hrow[6]; // 커버는 갤러리로

        // 2) 룸타입 오퍼 (✅ guests 바인딩)
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(ROOMTYPE_SQL)
                .setParameter("hid", hotelId)
                .setParameter("ci", Date.valueOf(ci))
                .setParameter("co", Date.valueOf(co))
                .setParameter("guests", Math.max(1, guests))  // ✅ 최소 1 보정
                .getResultList();

        List<HotelDetailsDto.RoomTypeOffer> types = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            HotelDetailsDto.RoomTypeOffer t = new HotelDetailsDto.RoomTypeOffer();
            t.setRoomTypeId(nLong(r[0]));
            t.setName((String) r[1]);
            t.setCapacity(nInt(r[2]));
            t.setAreaSqm(nInt(r[3]));
            t.setMinRemaining(nInt(r[4]));
            t.setPriceSum(nLong(r[5]));
            t.setNights(nInt(r[6]));
            t.setTemplateImageUrl((String) r[7]);
            types.add(t);
        }

        // 3) 갤러리: cover + 기본 객실 이미지
        @SuppressWarnings("unchecked")
        List<Object[]> photoRows = em.createNativeQuery(DEFAULT_ROOM_PHOTOS_SQL).getResultList();

        List<HotelDetailsDto.RoomImage> defaultImages = new ArrayList<>(photoRows.size());
        for (Object[] r : photoRows) {
            HotelDetailsDto.RoomImage img = new HotelDetailsDto.RoomImage();
            img.setUrl((String) r[0]);
            img.setAltText((String) r[1]);
            img.setSortOrder(nInt(r[2]));
            defaultImages.add(img);
        }

        HotelDetailsDto.Gallery g = new HotelDetailsDto.Gallery();
        g.setCover(cover);
        g.setRoomDefaults(defaultImages);

        // 4) 조립
        HotelDetailsDto dto = new HotelDetailsDto();
        dto.setHotel(h);
        dto.setGallery(g);
        dto.setRoomTypes(types);
        dto.setAmenities(List.of());
        return dto;
    }
}
