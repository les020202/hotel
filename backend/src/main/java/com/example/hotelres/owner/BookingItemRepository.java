// src/main/java/com/example/hotelres/owner/BookingItemRepository.java
package com.example.hotelres.owner;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingItemRepository extends JpaRepository<BookingItemEntity, Long> {

    // booking (연관 엔티티)의 id로 조회
    List<BookingItemEntity> findByBooking_Id(Long bookingId);
}
