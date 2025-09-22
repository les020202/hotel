// path: src/main/java/com/example/hotelres/reservation/ReservationRepository.java
package com.example.hotelres.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> { }
