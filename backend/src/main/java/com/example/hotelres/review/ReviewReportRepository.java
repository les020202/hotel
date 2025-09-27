// src/main/java/com/example/hotelres/review/ReviewReportRepository.java
package com.example.hotelres.review;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    boolean existsByReviewIdAndReporterId(Long reviewId, Long reporterId);

    /* 관리자 그리드용 Native DTO 프로젝션 (snake_case alias 매칭) */
    interface AdminReportRow {
        Long   getReport_id();   // 신고 ID
        Long   getId();          // 리뷰 ID
        String getHotel_name();  // 호텔명
        String getUser_name();   // 리뷰 작성자
        Short  getRating();
        String getComment();
        String getReporter_name();
        String getReporter_role();
        String getReason();
        String getReported_at(); // 'YYYY-MM-DD HH:mm'
    }

    @Query(value = """
        SELECT
            rr.id                                        AS report_id,
            r.id                                         AS id,
            h.name                                       AS hotel_name,
            u.name                                       AS user_name,
            r.rating                                     AS rating,
            r.comment                                    AS comment,
            COALESCE(ru.name, '(알 수 없음)')            AS reporter_name,
            rr.reporter_role                             AS reporter_role,
            CONCAT(
                COALESCE(rr.reason, ''),
                IF(rr.detail IS NULL OR rr.detail='' , '', CONCAT(' / ', rr.detail))
            )                                            AS reason,
            DATE_FORMAT(rr.created_at, '%Y-%m-%d %H:%i') AS reported_at
        FROM review_reports rr
        JOIN reviews r   ON r.id  = rr.review_id
        JOIN hotels  h   ON h.id  = r.hotel_id
        JOIN users   u   ON u.id  = r.user_id
        LEFT JOIN users ru ON ru.id = rr.reporter_id
        WHERE
            (:minRating IS NULL OR r.rating >= :minRating)
        AND (:maxRating IS NULL OR r.rating <= :maxRating)
        AND (:ownerOnly = FALSE OR rr.reporter_role = 'OWNER')   -- ★ 수정: ROLE_OWNER 제거
        AND (
              :q IS NULL
              OR LOWER(h.name)    LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(u.name)    LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(r.comment) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(ru.name)   LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(rr.reason) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(rr.detail) LIKE LOWER(CONCAT('%', :q, '%'))
        )
        ORDER BY rr.id DESC
        LIMIT 1000
        """, nativeQuery = true)
    List<AdminReportRow> findAdminReports(
            @Param("minRating") Integer minRating,
            @Param("maxRating") Integer maxRating,
            @Param("ownerOnly") boolean ownerOnly,
            @Param("q") String q
    );

    /* 신고 멱등 저장: 이미 있으면 0, 새로 저장되면 1 반환 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
  INSERT IGNORE INTO review_reports
    (review_id, reporter_id, reporter_role, reason, detail, status)
  VALUES
    (:reviewId, :reporterId, :reporterRole, :reason, :detail, 'PENDING')
  """, nativeQuery = true)
    int insertIgnore(@Param("reviewId") Long reviewId,
                     @Param("reporterId") Long reporterId,
                     @Param("reporterRole") String reporterRole,
                     @Param("reason") String reason,
                     @Param("detail") String detail);


    @Modifying
    @Query("DELETE FROM ReviewReport rr WHERE rr.reviewId = :reviewId")
    void deleteByReviewId(@Param("reviewId") Long reviewId);
}
