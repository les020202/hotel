package com.example.hotelres.main.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.hotelres.main.entity.BookingDayEntity;

import java.util.Collection;
import java.util.List;

public interface BookingDayRepository extends JpaRepository<BookingDayEntity, Long> {

    interface MinPriceRow {
        Long getHotelId();
        Integer getMinPrice();
    }

    @Query(value = """
        SELECT hotel_id AS hotelId, MIN(price) AS minPrice
        FROM booking_day
        WHERE hotel_id IN (:ids)
          AND is_sellable = 1
        GROUP BY hotel_id
        """, nativeQuery = true)
    List<MinPriceRow> findMinPriceByHotelIds(@Param("ids") Collection<Long> ids);
}
