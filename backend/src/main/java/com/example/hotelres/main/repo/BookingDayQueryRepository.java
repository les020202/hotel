package com.example.hotelres.main.repo;

import com.example.hotelres.main.entity.BookingDayEntity;
import com.example.hotelres.main.entity.BookingDayEntity.Status;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingDayQueryRepository extends JpaRepository<BookingDayEntity, Long> {

    /**
     * 호텔별 최저가 (오늘 이후, OPEN & 판매가능)
     * 결과: [0]=Long hotelId, [1]=Integer minPrice
     */
    @Query("""
       select b.hotel.id as hotelId, min(b.price) as minPrice
         from BookingDayEntity b
        where b.hotel.id in :ids
          and b.stayDate >= :today
          and b.status = :open
          and (b.isSellable is null or b.isSellable = true)
        group by b.hotel.id
    """)
    List<Object[]> findMinPriceByHotelIds(@Param("ids") List<Long> ids,
                                          @Param("today") LocalDate today,
                                          @Param("open") Status open);

    /**
     * 추천용: 호텔별 최저가 기준 오름차순
     * 결과: [0]=Long hotelId, [1]=Integer minPrice
     */
    @Query("""
       select b.hotel.id as hotelId, min(b.price) as minPrice
         from BookingDayEntity b
        where b.stayDate >= :today
          and b.status = :open
          and (b.isSellable is null or b.isSellable = true)
        group by b.hotel.id
        order by minPrice asc
    """)
    List<Object[]> findCheapestHotelIds(@Param("today") LocalDate today,
                                        @Param("open") Status open);
    
    /* 카드(호텔 기본정보 + 최저가) */
    @Query("""
       select h.id as id,
              h.name as name,
              h.address as address,
              h.coverImageUrl as coverImageUrl,
              min(b.price) as minPrice
         from BookingDayEntity b
         join b.hotel h
        where b.stayDate >= :today
          and b.status = :open
          and (b.isSellable is null or b.isSellable = true)
        group by h.id, h.name, h.address, h.coverImageUrl
        order by minPrice asc
    """)
    List<Object[]> findRecommendedHotelCardRows(@Param("today") LocalDate today,
                                                @Param("open") Status open);
}
