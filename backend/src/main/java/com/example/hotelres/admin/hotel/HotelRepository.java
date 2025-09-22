package com.example.hotelres.admin.hotel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 레포지토리 파일
 * - HotelRepository (public)
 * - BookingRepository
 * - BookingDayRepository
 * - RoomTypeRepository
 * - RatePlanRepository
 * - HotelOwnerRepository
 *
 * 동일 패키지 내 서비스에서 그대로 주입해서 사용하세요.
 */

// ===== 1) 호텔 검색/목록 =====
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    @Query("""
      select h from Hotel h
      where (:region is null or :region = '' or lower(h.region) like lower(concat(:region, '%')))
        and (
          :q is null or :q = '' or
          lower(h.name) like lower(concat('%', :q, '%')) or
          lower(coalesce(h.address,'')) like lower(concat('%', :q, '%')) or
          lower(coalesce(h.phone,''))   like lower(concat('%', :q, '%'))
        )
      order by h.id desc
    """)
    Page<Hotel> search(@Param("region") String region,
                       @Param("q") String q,
                       Pageable pageable);
}


// ===== 2) 예약 존재 여부(삭제 보호용) =====
/*
 * interface BookingRepository extends JpaRepository<Booking, Long> {
 * 
 * // FK가 실제로 bookings.hotel_id 에 걸려 있어도, native 쿼리로 안전 체크
 * 
 * @Query(value = """ SELECT CASE WHEN COUNT(*)>0 THEN TRUE ELSE FALSE END FROM
 * bookings WHERE hotel_id = :hotelId """, nativeQuery = true) boolean
 * existsByHotelId(@Param("hotelId") Long hotelId); }
 * 
 * 
 * // ===== 3) 날짜별 재고/가격 테이블 정리 ===== interface BookingDayRepository extends
 * JpaRepository<BookingDay, Long> {
 * 
 * @Modifying
 * 
 * @Transactional
 * 
 * @Query(value = "DELETE FROM booking_day WHERE hotel_id = :hotelId",
 * nativeQuery = true) void deleteByHotelId(@Param("hotelId") Long hotelId); }
 * 
 * 
 * // ===== 4) 객실 타입 정리 ===== interface RoomTypeRepository extends
 * JpaRepository<RoomType, Long> {
 * 
 * @Modifying
 * 
 * @Transactional
 * 
 * @Query(value = "DELETE FROM room_types WHERE hotel_id = :hotelId",
 * nativeQuery = true) void deleteByHotelId(@Param("hotelId") Long hotelId); }
 * 
 * 
 * // ===== 5) 요금제 정리 ===== interface RatePlanRepository extends
 * JpaRepository<RatePlan, Long> {
 * 
 * @Modifying
 * 
 * @Transactional
 * 
 * @Query(value = "DELETE FROM rate_plans WHERE hotel_id = :hotelId",
 * nativeQuery = true) void deleteByHotelId(@Param("hotelId") Long hotelId); }
 * 
 * 
 * // ===== 6) 호텔-오너 매핑 정리 ===== interface HotelOwnerRepository extends
 * JpaRepository<HotelOwner, Long> {
 * 
 * @Modifying
 * 
 * @Transactional
 * 
 * @Query(value = "DELETE FROM hotel_owners WHERE hotel_id = :hotelId",
 * nativeQuery = true) void deleteByHotelId(@Param("hotelId") Long hotelId); }
 */
