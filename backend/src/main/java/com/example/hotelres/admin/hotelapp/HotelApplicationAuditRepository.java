// com.example.hotelres.admin.hotelapp.HotelApplicationAuditRepository
package com.example.hotelres.admin.hotelapp;

import com.example.hotelres.hotelapp.HotelApplicationEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface HotelApplicationAuditRepository extends JpaRepository<HotelApplicationEntity, Long> {

  @Query("""
    select h from HotelApplicationEntity h
     where (:status is null or h.status = :status)
       and (
         :q is null
         or lower(h.hotelName)  like lower(concat('%', :q, '%'))
         or lower(h.ownerName)  like lower(concat('%', :q, '%'))
         or lower(h.businessNo) like lower(concat('%', :q, '%'))
       )
     order by h.id desc
  """)
  Page<HotelApplicationEntity> adminSearch(
      @Param("status") HotelApplicationEntity.Status status,
      @Param("q") String q,
      Pageable pageable
  );
}
