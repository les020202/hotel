package com.example.hotelres.owner.inventory;

import com.example.hotelres.owner.inventory.dto.InventoryOverviewDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner/hotels/{hotelId}/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('OWNER')")
    public List<InventoryOverviewDto> overview(
            @PathVariable Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return inventoryService.getOverview(hotelId, from, to);
    }

    // 기존: GET /api/owner/hotels/{hotelId}/inventory?roomTypeId&from&to ... (그대로 유지)
    // 기존: PUT /api/owner/hotels/{hotelId}/inventory/bulk ... (그대로 유지)
}
