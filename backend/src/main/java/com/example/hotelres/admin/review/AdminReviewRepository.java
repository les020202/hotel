// src/main/java/.../admin/review/AdminReviewRepository.java
package com.example.hotelres.admin.review;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
public interface AdminReviewRepository extends JpaRepository<Review, Long> {

  /** 리뷰 리스트 행(사진/신고 개수 포함) */
  interface ReviewRow {
    Long getId();
    Long getHotelId();
    String getHotelName();
    Long getUserId();
    String getUserName();
    String getUserEmail();
    Integer getRating();
    String getComment();
    Integer getVisible();
    LocalDateTime getCreatedAt();
    Integer getPhotoCount();
    Integer getReportCount();
  }

  /** 신고 리스트 행(신고자/역할, 리뷰 요약 포함) */
  interface ReportRow {
    Long getReportId();
    Long getReviewId();
    Long getHotelId();
    String getHotelName();
    Long getReporterId();
    String getReporterName();
    String getReporterRole();
    String getReason();
    LocalDateTime getReportedAt();
    Integer getRating();
    String getComment();
  }

  /* ---- 리뷰 탭: 검색/필터 ---- */
  @Query(value = """
    SELECT
      r.id,
      r.hotel_id              AS hotelId,
      h.name                  AS hotelName,
      r.user_id               AS userId,
      u.name                  AS userName,
      u.email                 AS userEmail,
      r.rating,
      r.comment,
      r.visible,
      r.created_at            AS createdAt,
      (SELECT COUNT(1) FROM review_photos rp WHERE rp.review_id = r.id) AS photoCount,
      (SELECT COUNT(1) FROM review_reports rr WHERE rr.review_id = r.id) AS reportCount
    FROM reviews r
    JOIN hotels h ON h.id = r.hotel_id
    JOIN users  u ON u.id = r.user_id
    WHERE
      (:q IS NULL OR
        LOWER(h.name)   LIKE LOWER(CONCAT('%',:q,'%')) OR
        LOWER(u.name)   LIKE LOWER(CONCAT('%',:q,'%')) OR
        LOWER(u.email)  LIKE LOWER(CONCAT('%',:q,'%')) OR
        LOWER(r.comment)LIKE LOWER(CONCAT('%',:q,'%')))
      AND (:ratingMin IS NULL OR r.rating >= :ratingMin)
      AND (:ratingMax IS NULL OR r.rating <= :ratingMax)
      AND (:visible   IS NULL OR r.visible = :visible)
    ORDER BY r.created_at DESC
    LIMIT :limit OFFSET :offset
  """, nativeQuery = true)
  List<ReviewRow> findReviews(
      @Param("q") String q,
      @Param("ratingMin") Integer ratingMin,
      @Param("ratingMax") Integer ratingMax,
      @Param("visible") Integer visible,
      @Param("limit") int limit,
      @Param("offset") int offset);

  /* ---- 신고 탭: 검색/필터 ---- */
  @Query(value = """
    SELECT
      rr.id                     AS reportId,
      r.id                      AS reviewId,
      r.hotel_id                AS hotelId,
      h.name                    AS hotelName,
      rr.reporter_id            AS reporterId,
      xu.name                   AS reporterName,
      xu.role                   AS reporterRole,
      rr.reason,
      rr.created_at             AS reportedAt,
      r.rating,
      r.comment
    FROM review_reports rr
    JOIN reviews r ON r.id = rr.review_id
    JOIN hotels  h ON h.id = r.hotel_id
    JOIN users  xu ON xu.id = rr.reporter_id
    WHERE
      (:q IS NULL OR
        LOWER(h.name)      LIKE LOWER(CONCAT('%',:q,'%')) OR
        LOWER(xu.name)     LIKE LOWER(CONCAT('%',:q,'%')) OR
        LOWER(xu.email)    LIKE LOWER(CONCAT('%',:q,'%')) OR
        LOWER(rr.reason)   LIKE LOWER(CONCAT('%',:q,'%')) OR
        LOWER(r.comment)   LIKE LOWER(CONCAT('%',:q,'%')))
      AND (:ownerOnly = FALSE OR xu.role = 'ROLE_OWNER')
    ORDER BY rr.created_at DESC
    LIMIT :limit OFFSET :offset
  """, nativeQuery = true)
  List<ReportRow> findReports(
      @Param("q") String q,
      @Param("ownerOnly") boolean ownerOnly,
      @Param("limit") int limit,
      @Param("offset") int offset);

  /* 하드 삭제 */
  @Transactional
  @Modifying
  @Query(value = "DELETE FROM reviews WHERE id=:id", nativeQuery = true)
  int hardDelete(@Param("id") Long id);
}
