// repo/PayoutTransactionRepository.java
package com.example.hotelres.settlement.repo;

import com.example.hotelres.settlement.entity.PayoutTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutTransactionRepository extends JpaRepository<PayoutTransaction, Long> {
	boolean existsByStatementId(Long statementId);
}
