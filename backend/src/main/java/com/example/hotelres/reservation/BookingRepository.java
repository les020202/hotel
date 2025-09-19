//src/main/java/com/example/hotelres/reservation/BookingRepository.java
package com.example.hotelres.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByVoucherNo(String voucherNo); // ✅ orderId 대신 voucherNo 사용
}