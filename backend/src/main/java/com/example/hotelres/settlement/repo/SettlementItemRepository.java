// repo/SettlementItemRepository.java
package com.example.hotelres.settlement.repo;

import com.example.hotelres.settlement.entity.SettlementItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementItemRepository extends JpaRepository<SettlementItem, Long> {
  void deleteByStatementId(Long statementId);
  long countByStatementId(Long statementId);
}
