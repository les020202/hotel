package com.example.hotelres.owner.inventory.dto;

public record InventoryRegisterResult(
        int totalRequested,
        int created,
        int skippedExisting,
        int skippedOutOfRange
){}
