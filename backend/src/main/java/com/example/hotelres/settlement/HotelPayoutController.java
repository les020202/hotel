// src/main/java/com/example/hotelres/hotel/HotelPayoutController.java
package com.example.hotelres.settlement;

import com.example.hotelres.settlement.dto.PayoutInfoDTO;
import com.example.hotelres.settlement.repo.HotelPayoutLookupRepository;
import com.example.hotelres.settlement.repo.SettlementStatementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelPayoutController {

    private final SettlementStatementRepository stmtRepo;
    private final HotelPayoutLookupRepository hotelPayoutLookupRepo;

    /**
     * 호텔의 지급정보 기본값을 조회한다.
     * 1) 가장 최근 정산서 스냅샷
     * 2) 없으면 hotels 테이블의 기본값
     * 3) 둘 다 없으면 null
     */
    @GetMapping(value = "/{id}/payout", produces = "application/json")
    public PayoutInfoDTO getPayout(@PathVariable("id") Long hotelId) {
        // 1) 최근 정산서 스냅샷 → DTO 매핑
        PayoutInfoDTO fromStatement = stmtRepo.findTop1ByHotelIdOrderByCreatedAtDesc(hotelId)
            .map(s -> new PayoutInfoDTO(
                    s.getPayoutBankCode(),
                    s.getPayoutAccountNo(),
                    s.getPayoutHolderName()
            ))
            .orElse(null);

        if (fromStatement != null) return fromStatement;

        // 2) hotels 기본값 → DTO 매핑
        return hotelPayoutLookupRepo.findPayoutByHotelId(hotelId)
            .map(v -> new PayoutInfoDTO(
                    v.getPayoutBankCode(),
                    v.getPayoutAccountNo(),
                    v.getPayoutHolderName()
            ))
            .orElse(null);
    }
}
