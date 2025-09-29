package com.example.hotelres.owner.inventory;

import com.example.hotelres.owner.inventory.dto.InventoryRegisterRequest;
import com.example.hotelres.owner.inventory.dto.InventoryRegisterResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner/hotels/{hotelId}/inventory")
public class InventoryRegisterController {

    private final InventoryRegisterService registerService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('OWNER')")
    public InventoryRegisterResult register(@PathVariable Long hotelId,
                                            @RequestBody InventoryRegisterRequest req) {
        // (선택) 여기서 호텔-오너 권한 검증 추가
        return registerService.register(hotelId, req);
    }
}
