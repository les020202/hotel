package com.example.hotelres.admin.hotelapp;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelApplicationAuditRepository extends JpaRepository<HotelApplicationAudit, Long> {
}
