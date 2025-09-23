package com.example.hotelres.reservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingHoldRepository extends JpaRepository<BookingHold, Long> {
    boolean existsByHoldCode(String holdCode);
    BookingHold findByHoldCode(String holdCode);
    //  만료된 홀드 조회용
    List<BookingHold> findAllByExpiresAtBefore(LocalDateTime now);

    // ✅ 재가격/동시요청 시 금액 갱신 원자성 보장을 위한 PESSIMISTIC WRITE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from BookingHold h where h.holdCode = :holdCode")
    Optional<BookingHold> findForUpdateByHoldCode(@Param("holdCode") String holdCode);
}
