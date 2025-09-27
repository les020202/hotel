// src/main/java/com/example/hotelres/admin/hotelapp/HotelApplicationAuditService.java
package com.example.hotelres.admin.hotelapp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class HotelApplicationAuditService {

    private final HotelApplicationAuditRepository repo;

    public HotelApplicationAuditService(HotelApplicationAuditRepository repo) {
        this.repo = repo;
    }

    /** 승인 → 프로시저가 내부에서 트랜잭션 처리하지만, 서비스도 경계 유지 */
    @Transactional
    public long approve(long applicationId, long adminId) {
        return repo.callApproveProcedure(applicationId, adminId);
    }

    /** 반려 → 사유 저장 */
    @Transactional
    public void reject(long applicationId, long adminId, String memo) {
        int updated = repo.reject(applicationId, adminId, memo);
        if (updated == 0) {
            throw new IllegalStateException("반려 불가 상태거나 존재하지 않는 신청입니다.");
        }
    }

    public HotelAppRow getOne(long id) { return repo.findOne(id); }

    public List<HotelAppRow> search(String status, String q) { return repo.search(status, q); }

    /* ====== DTOs / Mappers ====== */

    public record ApproveResponse(long hotelId) {}
    public record RejectRequest(String memo) {}
    public record ApproveRequest(String note) {}

    public static class HotelAppRow {
        public long id;
        public Long userId;
        public String ownerName;
        public String businessNo;
        public String phone;
        public Integer gradeLevel;
        public String hotelName;
        public String address1;
        public String address2;
        public String postcode;
        public String amenitiesCsv;
        public String comment;
        public String roomsJson;
        public String status;
        public String reviewMemo;
        public Long reviewedBy;
        public LocalDateTime reviewedAt;
        public Long approvedHotelId;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
        public String coverImageType;
        public String coverImageUrl;
        public String coverImageTemplate;
        public String region;

        static HotelAppRow from(ResultSet rs) throws SQLException {
            var r = new HotelAppRow();
            return map(rs, r);
        }
        static HotelAppRow mapRow(ResultSet rs, int rowNum) throws SQLException { return from(rs); }
        private static HotelAppRow map(ResultSet rs, HotelAppRow r) throws SQLException {
            r.id = rs.getLong("id");
            r.userId = rs.getLong("user_id");
            if (rs.wasNull()) r.userId = null;
            r.ownerName = rs.getString("owner_name");
            r.businessNo = rs.getString("business_no");
            r.phone = rs.getString("phone");
            r.gradeLevel = (Integer) rs.getObject("grade_level");
            r.hotelName = rs.getString("hotel_name");
            r.address1 = rs.getString("address1");
            r.address2 = rs.getString("address2");
            r.postcode = rs.getString("postcode");
            r.amenitiesCsv = rs.getString("amenities_csv");
            r.comment = rs.getString("comment");
            r.roomsJson = rs.getString("rooms_json");
            r.status = rs.getString("status");
            r.reviewMemo = rs.getString("review_memo");
            r.reviewedBy = (Long) rs.getObject("reviewed_by");
            r.reviewedAt = rs.getTimestamp("reviewed_at") != null ? rs.getTimestamp("reviewed_at").toLocalDateTime() : null;
            r.approvedHotelId = (Long) rs.getObject("approved_hotel_id");
            r.createdAt = rs.getTimestamp("created_at").toLocalDateTime();
            r.updatedAt = rs.getTimestamp("updated_at").toLocalDateTime();
            r.coverImageType = rs.getString("cover_image_type");
            r.coverImageUrl = rs.getString("cover_image_url");
            r.coverImageTemplate = rs.getString("cover_image_template");
            r.region = rs.getString("region");
            return r;
        }
    }
}
