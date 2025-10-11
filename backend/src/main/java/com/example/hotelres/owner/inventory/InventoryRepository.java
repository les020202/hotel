package com.example.hotelres.owner.inventory;

import com.example.hotelres.main.entity.BookingDayEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InventoryRepository extends Repository<BookingDayEntity, Long> {

    @Query(value = """
        WITH RECURSIVE d AS (
          SELECT DATE(:from) AS dt
          UNION ALL
          SELECT DATE_ADD(dt, INTERVAL 1 DAY) FROM d WHERE dt < DATE(:to)
        )
        SELECT
          d.dt                         AS stay_date,       -- [0] java.sql.Date
          rt.id                        AS room_type_id,    -- [1] Long
          rt.type_code                 AS type_code,       -- [2] String
          bd.price                     AS price,           -- [3] Integer (NULL if no row)
          bd.allotment                 AS allotment,       -- [4] Integer (NULL if no row)
          bd.booked                    AS booked,          -- [5] Integer (NULL if no row)
          bd.status                    AS status,          -- [6] String  (NULL if no row)
          (bd.id IS NOT NULL)          AS has_data         -- [7] Boolean/Number
        FROM d
        JOIN room_types rt
          ON rt.hotel_id = :hotelId
        LEFT JOIN booking_day bd
          ON bd.hotel_id     = :hotelId
         AND bd.room_type_id = rt.id
         AND bd.stay_date    = d.dt
        ORDER BY d.dt, rt.type_code
        """, nativeQuery = true)
    List<Object[]> findOverviewRows(@Param("hotelId") Long hotelId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);
}
