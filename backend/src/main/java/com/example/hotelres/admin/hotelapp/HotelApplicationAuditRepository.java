// src/main/java/com/example/hotelres/admin/hotelapp/HotelApplicationAuditRepository.java
package com.example.hotelres.admin.hotelapp;

import java.sql.Types;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import com.example.hotelres.admin.hotelapp.HotelApplicationAuditService.HotelAppRow;

import jakarta.annotation.PostConstruct;

@Repository
public class HotelApplicationAuditRepository {

    private final JdbcTemplate jdbc;
    private SimpleJdbcCall approveCall;

    public HotelApplicationAuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    
    @PostConstruct
    void init() {
    this.approveCall = new SimpleJdbcCall(jdbc)
        .withCatalogName("hotelres")                 
        .withProcedureName("approve_hotel_application")
        .withoutProcedureColumnMetaDataAccess()
        .declareParameters(
            new SqlParameter("p_application_id", Types.BIGINT),
            new SqlParameter("p_admin_id", Types.BIGINT),
            new SqlOutParameter("o_hotel_id", Types.BIGINT)
        );
    }

    public long callApproveProcedure(long applicationId, long adminId) {
        MapSqlParameterSource in = new MapSqlParameterSource()
            .addValue("p_application_id", applicationId)
            .addValue("p_admin_id", adminId);
        var out = approveCall.execute(in);
        Object v = out.get("o_hotel_id");
        if (v == null) throw new IllegalStateException("approve_hotel_application returned null hotelId");
        return ((Number) v).longValue();
    }

    /** 반려 처리(사유 저장 포함) */
    public int reject(long applicationId, long adminId, String memo) {
        // 상태 전이: PENDING/UNDER_REVIEW/NEEDS_INFO 만 반려 허용
        String sql = """
            UPDATE hotel_applications
               SET status = 'REJECTED',
                   review_memo = ?,
                   reviewed_by = ?,
                   reviewed_at = NOW(),
                   updated_at = NOW()
             WHERE id = ?
               AND status IN ('PENDING','UNDER_REVIEW','NEEDS_INFO')
            """;
        return jdbc.update(sql, memo, adminId, applicationId);
    }

    /** 단건 조회(상세 모달용) */
    public HotelAppRow findOne(long id) {
        String sql = """
            SELECT id, user_id, owner_name, business_no, phone, grade_level,
                   hotel_name, address1, address2, postcode,
                   amenities_csv, comment, rooms_json,
                   status, review_memo, reviewed_by, reviewed_at,
                   approved_hotel_id, created_at, updated_at,
                   cover_image_type, cover_image_url, cover_image_template, region
              FROM hotel_applications
             WHERE id = ?
            """;
        return jdbc.query(sql, rs -> rs.next() ? HotelAppRow.from(rs) : null, id);
    }

    /** 목록 조회(간단 버전 – 필요하면 페이징 쿼리로 바꿔도 됨) */
    public java.util.List<HotelAppRow> search(String status, String q) {
        String base = """
          SELECT id, user_id, owner_name, business_no, phone, grade_level,
                 hotel_name, address1, address2, postcode,
                 amenities_csv, comment, rooms_json,
                 status, review_memo, reviewed_by, reviewed_at,
                 approved_hotel_id, created_at, updated_at,
                 cover_image_type, cover_image_url, cover_image_template, region
            FROM hotel_applications
           WHERE 1=1
          """;
        var args = new java.util.ArrayList<>();
        var sb = new StringBuilder(base);
        if (status != null && !status.isBlank()) {
            sb.append(" AND status = ? ");
            args.add(status);
        }
        if (q != null && !q.isBlank()) {
            sb.append("""
              AND (hotel_name LIKE CONCAT('%',?,'%')
                   OR owner_name LIKE CONCAT('%',?,'%')
                   OR business_no LIKE CONCAT('%',?,'%'))
            """);
            args.add(q); args.add(q); args.add(q);
        }
        sb.append(" ORDER BY created_at DESC LIMIT 200 ");
        return jdbc.query(sb.toString(), args.toArray(), HotelAppRow::mapRow);
    }
}
