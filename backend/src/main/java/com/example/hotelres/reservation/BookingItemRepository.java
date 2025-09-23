// src/main/java/com/example/hotelres/reservation/BookingItemRepository.java
package com.example.hotelres.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {}
