// src/main/java/com/example/hotelres/review/ReviewRepository.java
package com.example.hotelres.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByUserIdAndBookingId(Long userId, Long bookingId);

    @Query("""
      select r from Review r
      where r.hotelId = :hotelId
        and r.visible = true
      order by r.createdAt desc
    """)
    Page<Review> findVisibleByHotel(@Param("hotelId") Long hotelId, Pageable pageable);

    @Query("""
      select avg(r.rating) from Review r
      where r.hotelId = :hotelId and r.visible = true
    """)
    Double avgRating(@Param("hotelId") Long hotelId);

    @Query("""
      select count(r) from Review r
      where r.hotelId = :hotelId and r.visible = true
    """)
    long countVisible(@Param("hotelId") Long hotelId);

    List<Review> findByHotelIdAndVisibleTrueOrderByCreatedAtDesc(Long hotelId);

    /* =========================
       (기존) 관리자/오너 공통 검색 (엔티티 반환)
       ========================= */
    @Query("""
        select r from Review r
        where (:hotelId is null or r.hotelId = :hotelId)
          and (:visible is null or r.visible = :visible)
          and (:fromAt is null or r.createdAt >= :fromAt)
          and (:toAt   is null or r.createdAt <  :toAt)
          and (
               :q is null
               or lower(r.comment) like lower(concat('%', :q, '%'))
          )
        order by r.id desc
    """)
    Page<Review> searchForManage(@Param("hotelId") Long hotelId,
                                 @Param("visible") Boolean visible,
                                 @Param("fromAt") LocalDateTime fromAt,
                                 @Param("toAt") LocalDateTime toAt,
                                 @Param("q") String q,
                                 Pageable pageable);

    /* ============================================================
       ✅ 관리자/오너 그리드용(집계 + 호텔명/작성자명 포함)
       - snake_case alias 사용 (native + interface projection 안정화)
       ============================================================ */
    @Query(value = """
        SELECT
          r.id                                                     AS id,
          r.hotel_id                                               AS hotel_id,
          r.user_id                                                AS user_id,
          h.name                                                   AS hotel_name,
          u.name                                                   AS user_name,
          r.rating                                                 AS rating,
          r.comment                                                AS comment,
          r.visible                                                AS visible,
          r.created_at                                             AS created_at,
          COALESCE(COUNT(rr.id), 0)                                AS report_count,
          COALESCE(SUM(rr.reporter_role = 'USER'),  0)             AS user_report_count,
          COALESCE(SUM(rr.reporter_role IN ('OWNER','ROLE_OWNER')),0) AS owner_report_count,
          MAX(rr.created_at)                                       AS latest_report_at
        FROM reviews r
        JOIN hotels h ON h.id = r.hotel_id
        JOIN users  u ON u.id = r.user_id
        LEFT JOIN review_reports rr ON rr.review_id = r.id
        WHERE (:hotelId IS NULL OR r.hotel_id = :hotelId)
          AND (:visible IS NULL OR r.visible = :visible)
          AND (:fromAt  IS NULL OR r.created_at >= :fromAt)
          AND (:toAt    IS NULL OR r.created_at <  :toAt)
          AND (:q IS NULL OR LOWER(r.comment) LIKE CONCAT('%', LOWER(:q), '%'))
        GROUP BY r.id, r.hotel_id, r.user_id, h.name, u.name, r.rating, r.comment, r.visible, r.created_at
        HAVING (:reportedOnly IS NULL OR :reportedOnly = FALSE OR COALESCE(COUNT(rr.id),0) > 0)
        ORDER BY r.id DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM (
          SELECT r.id
          FROM reviews r
          JOIN hotels h ON h.id = r.hotel_id
          JOIN users  u ON u.id = r.user_id
          LEFT JOIN review_reports rr ON rr.review_id = r.id
          WHERE (:hotelId IS NULL OR r.hotel_id = :hotelId)
            AND (:visible IS NULL OR r.visible = :visible)
            AND (:fromAt  IS NULL OR r.created_at >= :fromAt)
            AND (:toAt    IS NULL OR r.created_at <  :toAt)
            AND (:q IS NULL OR LOWER(r.comment) LIKE CONCAT('%', LOWER(:q), '%'))
          GROUP BY r.id
          HAVING (:reportedOnly IS NULL OR :reportedOnly = FALSE OR COALESCE(COUNT(rr.id),0) > 0)
        ) x
        """,
            nativeQuery = true)
    Page<ReviewManageRow> searchManageWithReports(@Param("hotelId") Long hotelId,
                                                  @Param("visible") Boolean visible,
                                                  @Param("reportedOnly") Boolean reportedOnly,
                                                  @Param("fromAt") LocalDateTime fromAt,
                                                  @Param("toAt") LocalDateTime toAt,
                                                  @Param("q") String q,
                                                  Pageable pageable);

    /* 단건 상세(집계 포함) */
    @Query(value = """
        SELECT
          r.id                                                     AS id,
          r.hotel_id                                               AS hotel_id,
          r.user_id                                                AS user_id,
          h.name                                                   AS hotel_name,
          u.name                                                   AS user_name,
          r.rating                                                 AS rating,
          r.comment                                                AS comment,
          r.visible                                                AS visible,
          r.created_at                                             AS created_at,
          COALESCE(COUNT(rr.id), 0)                                AS report_count,
          COALESCE(SUM(rr.reporter_role = 'USER'),  0)             AS user_report_count,
          COALESCE(SUM(rr.reporter_role IN ('OWNER','ROLE_OWNER')),0) AS owner_report_count,
          MAX(rr.created_at)                                       AS latest_report_at
        FROM reviews r
        JOIN hotels h ON h.id = r.hotel_id
        JOIN users  u ON u.id = r.user_id
        LEFT JOIN review_reports rr ON rr.review_id = r.id
        WHERE r.id = :reviewId
        GROUP BY r.id, r.hotel_id, r.user_id, h.name, u.name, r.rating, r.comment, r.visible, r.created_at
        """, nativeQuery = true)
    ReviewManageRow findManageRowById(@Param("reviewId") Long reviewId);

    /* 네이티브 프로젝션 인터페이스 — snake_case 이름과 정확히 매칭 */
    interface ReviewManageRow {
        Long getId();
        Long getHotel_id();
        Long getUser_id();
        String getHotel_name();
        String getUser_name();

        Short getRating();
        String getComment();
        Boolean getVisible();
        LocalDateTime getCreated_at();

        Integer getReport_count();
        Integer getUser_report_count();
        Integer getOwner_report_count();
        LocalDateTime getLatest_report_at();
    }
    Page<Review> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
