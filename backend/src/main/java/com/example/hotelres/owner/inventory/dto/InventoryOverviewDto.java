package com.example.hotelres.owner.inventory.dto;

import java.time.LocalDate;
import java.util.List;

public record InventoryOverviewDto(
        LocalDate date,
        List<Item> items
) {
    public record Item(
            Long roomTypeId,
            String typeCode,    // STANDARD/DELUXE/SUITE/PREMIUM
            Integer price,
            Integer allotment,
            Integer booked,
            Integer remaining,  // GREATEST(allotment - booked, 0)
            String status       // OPEN/CLOSED
    ) {}
}
