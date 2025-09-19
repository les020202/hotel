package com.example.hotelres.admin.hotelapp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.example.hotelres.admin.hotelapp.HotelApplication.Status;

import java.util.Optional;

public interface HotelApplicationRepository extends JpaRepository<HotelApplication, Long> {

    Optional<HotelApplication> findByCanonicalKey(String canonicalKey);

    @Query("""
        SELECT a FROM HotelApplication a
         WHERE (:status IS NULL OR a.status = :status)
           AND (:region IS NULL OR a.region = :region)
           AND (:q IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(a.address) LIKE LOWER(CONCAT('%', :q, '%')))
         ORDER BY a.id DESC
    """)
    Page<HotelApplication> search(
            @Param("status") Status status,
            @Param("region") String region,
            @Param("q") String q,
            Pageable pageable
    );
}
