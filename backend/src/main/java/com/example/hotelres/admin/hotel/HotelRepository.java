package com.example.hotelres.admin.hotel;

import java.util.List;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    @Query("""
      select h from Hotel h
      where (:region is null or :region = '' or lower(h.region) like lower(concat('%', :region, '%')))
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
    
    @Query("select h.id from Hotel h")
    List<Long> findAllHotelIds();
}
