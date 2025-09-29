package com.example.hotelres.owner.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // 클라이언트가 추가 필드 보내도 무시
public record InventoryRegisterRequest(
        Long roomTypeId,            // 단일 선택 시
        List<Long> roomTypeIds,     // 다중 선택 시
        LocalDate from,
        LocalDate to,
        List<Integer> weekdays,     // 1=Mon..7=Sun (null/빈값이면 전체)
        Integer price,
        Integer allotment,
        String status,              // OPEN/CLOSED (SOLD_OUT은 파생)
        Boolean overwrite           // ← 덮어쓰기 여부(null이면 false로 처리)
) {}
