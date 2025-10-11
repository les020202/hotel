package com.example.hotelres.hotelapp;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelApplicationRepository extends JpaRepository<HotelApplicationEntity, Long> {
    Page<HotelApplicationEntity> findAllByOrderByIdDesc(Pageable pageable);
    Page<HotelApplicationEntity> findByStatusOrderByIdDesc(HotelApplicationEntity.Status status, Pageable pageable);
    Page<HotelApplicationEntity> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
