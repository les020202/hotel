package com.example.hotelres.owner.inventory;

import com.example.hotelres.owner.inventory.dto.InventoryOverviewDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public List<InventoryOverviewDto> getOverview(Long hotelId, LocalDate from, LocalDate to) {
        var rows = inventoryRepository.findOverviewRows(hotelId, from, to);

        Map<LocalDate, List<InventoryOverviewDto.Item>> map = new LinkedHashMap<>();

        for (Object[] r : rows) {
            // [0]=stay_date, [1]=room_type_id, [2]=type_code,
            // [3]=price, [4]=allotment, [5]=booked, [6]=persisted status, [7]=has_data
            LocalDate date   = ((java.sql.Date) r[0]).toLocalDate();
            Long roomTypeId  = ((Number) r[1]).longValue();
            String typeCode  = (String) r[2];

            // booking_day 존재 여부
            boolean hasData = false;
            if (r.length >= 8) {
                Object v = r[7];
                hasData = (v instanceof Boolean) ? (Boolean) v
                        : (v instanceof Number) ? (((Number) v).intValue() != 0)
                        : v != null;
            }

            // ◀ 핵심: booking_day가 없으면 개요 아이템을 생성하지 않는다 (프론트는 '—'로 표시)
            if (!hasData) continue;

            Integer priceVal = (r[3] == null) ? null : ((Number) r[3]).intValue();
            int allot        = (r[4] == null) ? 0    : ((Number) r[4]).intValue();
            int booked       = (r[5] == null) ? 0    : ((Number) r[5]).intValue();
            String persisted = (String) r[6]; // OPEN / CLOSED / (nullable)

            int remaining = Math.max(allot - booked, 0);

            // 파생 상태: CLOSED 우선, 아니면 재고로 SOLD_OUT/OPEN 판정
            String status = ("CLOSED".equalsIgnoreCase(persisted)) ? "CLOSED"
                    : (remaining == 0 ? "SOLD_OUT" : "OPEN");

            map.computeIfAbsent(date, k -> new ArrayList<>())
                    .add(new InventoryOverviewDto.Item(
                            roomTypeId,
                            typeCode,
                            priceVal,     // Integer nullable로 두어 프론트가 '—' 처리 가능
                            allot,
                            booked,
                            remaining,
                            status
                    ));
        }

        List<InventoryOverviewDto> out = new ArrayList<>();
        map.forEach((d, items) -> out.add(new InventoryOverviewDto(d, items)));
        return out;
    }
}
