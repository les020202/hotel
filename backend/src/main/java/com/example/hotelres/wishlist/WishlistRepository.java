// src/main/java/com/example/hotelres/wishlist/WishlistRepository.java
package com.example.hotelres.wishlist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/*
 * WishlistRepository
 * - Wishlist 엔티티에 대한 JPA 리포지토리 인터페이스
 * - 커스텀 프로젝션(Row)과 JPQL을 사용해 목록용 DTO 데이터를 바로 조회합니다.
 */
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

  /*
   * Row
   * - JPQL select 절의 별칭과 매칭되는 인터페이스 기반 프로젝션
   * - 메서드 이름은 select 별칭과 동일해야 자동 매핑됩니다.
   */
  interface Row {
    Long getWishlistId();        // w.id as wishlistId
    LocalDateTime getCreatedAt();// w.createdAt as createdAt

    Long getHotelId();           // h.id as hotelId
    String getName();            // h.name as name
    String getRegion();          // h.region as region
    String getAddress();         // h.address as address
    BigDecimal getRating();      // h.rating as rating (DB DECIMAL → BigDecimal)
    Integer getGradeLevel();     // h.gradeLevel as gradeLevel
    String getCoverImageUrl();   // h.coverImageUrl as coverImageUrl
  }

  /*
   * findRowsByUserId
   * - 특정 사용자(userId)의 위시리스트를 페이지네이션으로 조회
   * - Wishlist w 를 기준으로 호텔(h)과 조인하여 화면에 필요한 요약 필드만 선택
   * - 결과는 Row 프로젝션으로 반환됩니다.
   */
  @Query("""
    select
      w.id as wishlistId,
      w.createdAt as createdAt,
      h.id as hotelId,
      h.name as name,
      h.region as region,
      h.address as address,
      h.rating as rating,
      h.gradeLevel as gradeLevel,
      h.coverImageUrl as coverImageUrl
    from Wishlist w
    join w.hotel h
    where w.user.id = :userId
    order by w.createdAt desc
  """)
  Page<Row> findRowsByUserId(@Param("userId") Long userId, Pageable pageable);

  /*
   * findByIdAndUserId
   * - 위시리스트 항목의 소유자 검증을 위한 조회
   * - id와 userId가 모두 일치하는 단일 엔티티를 Optional로 반환합니다.
   */
  Optional<Wishlist> findByIdAndUserId(Long id, Long userId);
}
