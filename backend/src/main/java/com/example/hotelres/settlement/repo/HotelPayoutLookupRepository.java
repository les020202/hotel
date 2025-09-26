// src/main/java/com/example/hotelres/settlement/repo/HotelPayoutLookupRepository.java
package com.example.hotelres.settlement.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 호텔 기준 지급정보(은행/계좌/예금주) 단건 조회 전용 리포지토리.
 * Spring Data Repository<T,ID>를 상속하지 않습니다.
 */
@Repository
public class HotelPayoutLookupRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public Optional<HotelPayoutView> findPayoutByHotelId(Long hotelId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT
                    h.payout_bank_code   AS payout_bank_code,
                    h.payout_account_no  AS payout_account_no,
                    h.payout_holder_name AS payout_holder_name
                FROM hotels h
                WHERE h.id = :hotelId
                """)
                .setParameter("hotelId", hotelId)
                .setMaxResults(1)   // 여기만 있으면 충분
                .getResultList();

        if (rows.isEmpty()) return Optional.empty();

        Object[] r = rows.get(0);
        String bankCode   = r[0] == null ? null : r[0].toString();
        String accountNo  = r[1] == null ? null : r[1].toString();
        String holderName = r[2] == null ? null : r[2].toString();

        return Optional.of(new HotelPayoutViewImpl(bankCode, accountNo, holderName));
    }

    /** 간단한 내부 구현체 */
    private record HotelPayoutViewImpl(
            String payoutBankCode,
            String payoutAccountNo,
            String payoutHolderName
    ) implements HotelPayoutView {
        @Override public String getPayoutBankCode()   { return payoutBankCode; }
        @Override public String getPayoutAccountNo()  { return payoutAccountNo; }
        @Override public String getPayoutHolderName() { return payoutHolderName; }
    }
}
