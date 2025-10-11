package com.example.hotelres.settlement;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.hotelres.settlement.entity.SettlementStatement;

//src/main/java/com/example/hotelres/settlement/repo/SettlementStatementRepository.java
//src/main/java/com/example/hotelres/settlement/SettlementStatementSumRepository.java
public interface SettlementStatementSumRepository extends JpaRepository<SettlementStatement, Long> {

@Query(value = """
  SELECT COALESCE(SUM(ss.payable_amount), 0)
  FROM settlement_statements ss
  WHERE ss.hotel_id = :hotelId
    AND ss.status = 'SETTLED'
    AND ss.period_start >= :start
    AND ss.period_end   <= :end
  """, nativeQuery = true)
Long sumSettledBetween(
  @Param("hotelId") Long hotelId,
  @Param("start") LocalDate start,
  @Param("end") LocalDate end
);
}

