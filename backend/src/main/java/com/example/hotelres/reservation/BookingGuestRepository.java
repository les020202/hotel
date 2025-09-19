// src/main/java/com/example/hotelres/reservation/BookingGuestRepository.java
package com.example.hotelres.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingGuestRepository extends JpaRepository<BookingGuest, Long> {
}
