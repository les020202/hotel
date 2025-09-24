// backend/src/main/java/com/example/hotelres/wishlist/WishlistRepository.java
package com.example.hotelres.wishlist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

  interface Row {
    Long getWishlistId();
    LocalDateTime getCreatedAt();

    Long getHotelId();
    String getName();
    String getRegion();
    String getAddress();
    BigDecimal getRating();
    Integer getGradeLevel();
    String getCoverImageUrl();
  }

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

  Optional<Wishlist> findByIdAndUserId(Long id, Long userId);

  Optional<Wishlist> findByUser_IdAndHotel_Id(Long userId, Long hotelId);
  boolean existsByUser_IdAndHotel_Id(Long userId, Long hotelId);
  void deleteByUser_IdAndHotel_Id(Long userId, Long hotelId);

  // ✅ 추가: 단일 위시리스트를 요약(Row)로 가져오기 (LAZY 안전)
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
    where w.id = :id
  """)
  Optional<Row> findRowById(@Param("id") Long id);
}
