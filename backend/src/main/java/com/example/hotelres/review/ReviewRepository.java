package com.example.hotelres.review;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

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
}
