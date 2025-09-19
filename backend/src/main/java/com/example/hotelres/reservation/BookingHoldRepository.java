// path: src/main/java/com/example/hotelres/reservation/BookingHoldRepository.java
package com.example.hotelres.reservation;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingHoldRepository extends JpaRepository<BookingHold, Long> {
    boolean existsByHoldCode(String holdCode);
    BookingHold findByHoldCode(String holdCode);
    //  만료된 홀드 조회용
    List<BookingHold> findAllByExpiresAtBefore(LocalDateTime now);
}
