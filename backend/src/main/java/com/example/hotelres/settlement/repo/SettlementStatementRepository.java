// src/main/java/com/example/hotelres/settlement/repo/SettlementStatementRepository.java
package com.example.hotelres.settlement.repo;

import com.example.hotelres.settlement.entity.SettlementStatement;
import com.example.hotelres.settlement.entity.enums.StatementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementStatementRepository extends JpaRepository<SettlementStatement, Long> {

    /* ====== 단건/존재여부 ====== */

    // 주차(기간) 단일 조회 – 생성 시 중복 방지용
    Optional<SettlementStatement> findByHotelIdAndPeriodStartAndPeriodEnd(
            Long hotelId, LocalDate start, LocalDate end);

    // 동일 키 존재 여부 (중복 생성 방지에 유용)
    boolean existsByHotelIdAndPeriodStartAndPeriodEnd(
            Long hotelId, LocalDate start, LocalDate end);

    // 최신 지급정보 스냅샷(은행/계좌/예금주) 가져오기
    Optional<SettlementStatement> findTop1ByHotelIdOrderByCreatedAtDesc(Long hotelId);

    /* ====== 목록 조회: 호텔별/기간 ====== */

    // 기본(오름차순 정렬은 서비스에서 정렬 지정 가능)
    List<SettlementStatement> findByHotelIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            Long hotelId, LocalDate start, LocalDate end);

    // 상태 포함
    List<SettlementStatement> findByHotelIdAndStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            Long hotelId, StatementStatus status, LocalDate start, LocalDate end);

    // 최신 생성순(프론트 테이블 최근 생성 먼저 보여줄 때 편리)
    List<SettlementStatement> findByHotelIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByCreatedAtDesc(
            Long hotelId, LocalDate start, LocalDate end);

    List<SettlementStatement> findByHotelIdAndStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByCreatedAtDesc(
            Long hotelId, StatementStatus status, LocalDate start, LocalDate end);

    /* ====== 목록 조회: 전체 호텔/기간 ====== */

    // 전체 + 상태
    List<SettlementStatement> findByStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            StatementStatus status, LocalDate start, LocalDate end);

    // 전체만
    List<SettlementStatement> findByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            LocalDate start, LocalDate end);

    // 전체 + 상태 (최신 생성순)
    List<SettlementStatement> findByStatusAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByCreatedAtDesc(
            StatementStatus status, LocalDate start, LocalDate end);

    // 전체 (최신 생성순)
    List<SettlementStatement> findByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqualOrderByCreatedAtDesc(
            LocalDate start, LocalDate end);
    
}
