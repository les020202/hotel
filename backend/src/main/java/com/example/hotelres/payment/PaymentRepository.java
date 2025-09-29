// path: src/main/java/com/example/hotelres/payment/PaymentRepository.java
package com.example.hotelres.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    // ✅ 컷오프 이전 승인(또는 생성)된 결제 총액 (온라인만 쓰므로 추가 필터 불필요)
    @Query(value = """
        SELECT COALESCE(SUM(p.amount), 0)
        FROM payments p
        JOIN bookings b ON b.id = p.booking_id
        WHERE b.hotel_id = :hotelId
          AND b.check_out BETWEEN :start AND :end
          AND COALESCE(p.approved_at, p.created_at) <= :cutoff
          -- 성공 상태 필터를 쓰고 싶으면 아래 주석 해제하고 실제 값에 맞추세요
          -- AND p.status IN ('APPROVED')
        """, nativeQuery = true)
    Long sumApprovedAmountBeforeCutoff(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("cutoff") LocalDateTime cutoff);
}
