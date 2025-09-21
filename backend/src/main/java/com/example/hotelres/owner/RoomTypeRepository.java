package com.example.hotelres.owner;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RoomTypeRepository extends JpaRepository<RoomTypeEntity, Long> {

    // 이미 room_types 테이블이 있으니, 엔티티가 없다면 간단한 뷰 엔티티를 만듭니다.
    @Query(value = """
      select rt.id as id, rt.name as name, rt.type_code as typeCode
      from room_types rt
      where rt.hotel_id = :hotelId
      order by rt.id
    """, nativeQuery = true)
    List<Object[]> findLiteByHotel(@Param("hotelId") Long hotelId);

    default List<RoomTypeLite> findLiteDtos(Long hotelId) {
        return findLiteByHotel(hotelId).stream()
                .map(a -> new RoomTypeLite(((Number)a[0]).longValue(), (String)a[1], (String)a[2]))
                .toList();
    }
}
