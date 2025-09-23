// path: src/main/java/com/example/hotelres/payment/PaymentRepository.java
package com.example.hotelres.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByProviderRef(String providerRef);
    Optional<Payment> findByProviderRef(String providerRef);
}
