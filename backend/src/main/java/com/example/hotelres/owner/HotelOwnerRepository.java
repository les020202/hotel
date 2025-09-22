package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.OwnerHotelView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HotelOwnerRepository extends JpaRepository<HotelOwner, Long> {

    // Guard 용 (login_id, hotel_id)
    boolean existsByUserLoginIdAndHotelId(String userLoginId, Long hotelId);

    // 내 호텔 ID만 뽑기 (Guard, 권한 필터링 등에 유용)
    @Query("select ho.hotelId from HotelOwner ho where ho.userLoginId = :loginId")
    List<Long> findHotelIdsByOwnerLoginId(@Param("loginId") String loginId);

    // 내 호텔 카드용 (Projection)
    @Query(value = """
        SELECT h.id                       AS id,
               h.name                     AS name,
               h.region                   AS region,
               COALESCE(h.grade_level,0)  AS gradeLevel,
               ho.business_no             AS businessNo
          FROM hotel_owners ho
          JOIN hotels h ON h.id = ho.hotel_id
         WHERE ho.user_login_id = :loginId
         ORDER BY h.id
        """, nativeQuery = true)
    List<OwnerHotelView> findHotelsByOwnerLogin(@Param("loginId") String loginId);
}
