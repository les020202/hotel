// src/main/java/com/example/hotelres/owner/HotelOwnerRepository.java
package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.OwnerHotelView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HotelOwnerRepository extends JpaRepository<HotelOwner, Long> {

    // 가드용 (두 시그니처 모두 제공: 혼선 방지. 하나만 쓰고 싶으면 아래 중 1개만 남겨도 됨)
    boolean existsByHotelIdAndUserLoginId(Long hotelId, String userLoginId);
    boolean existsByUserLoginIdAndHotelId(String userLoginId, Long hotelId);

    // 내 호텔 id 목록
    @Query("select ho.hotelId from HotelOwner ho where ho.userLoginId = :loginId")
    List<Long> findHotelIdsByOwnerLoginId(@Param("loginId") String loginId);

    // 오너 대시보드 카드/목록 프로젝션 (네이티브)
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
