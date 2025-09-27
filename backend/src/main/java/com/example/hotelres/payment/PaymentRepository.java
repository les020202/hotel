// path: src/main/java/com/example/hotelres/payment/PaymentRepository.java
package com.example.hotelres.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByProviderRef(String providerRef);
    Optional<Payment> findByProviderRef(String providerRef);

    // ✅ 특정 예약의 '성공' 결제 한 건 가져오기(가장 최근)
    Optional<Payment> findTopByBookingIdAndStatusOrderByIdDesc(Long bookingId, PaymentStatus status);

    // ✅ 특정 예약에 성공 결제가 존재하는지만 빠르게 확인
    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    // (옵션) 가장 최근 결제 아무 상태나
    Optional<Payment> findTopByBookingIdOrderByIdDesc(Long bookingId);
}
