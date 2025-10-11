package com.example.hotelres.owner.inventory;

import com.example.hotelres.owner.inventory.dto.InventoryRegisterRequest;
import com.example.hotelres.owner.inventory.dto.InventoryRegisterResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryRegisterService {

    private final InventoryRegisterRepository registerRepository;

    @Transactional
    public InventoryRegisterResult register(Long hotelId, InventoryRegisterRequest req) {
        // 날짜 제한: 오늘 ~ 오늘+30
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(30);

        LocalDate from = req.from().isBefore(today) ? today : req.from();
        LocalDate to   = req.to().isAfter(maxDate) ? maxDate : req.to();
        if (to.isBefore(from)) {
            return new InventoryRegisterResult(0, 0, 0, 0);
        }

        // 요일 필터 (1=Mon..7=Sun)
        Set<Integer> wd = new HashSet<>();
        if (req.weekdays() != null && !req.weekdays().isEmpty()) {
            wd.addAll(req.weekdays());
        }

        // 대상 룸타입
        List<Long> typeIds = (req.roomTypeIds() != null && !req.roomTypeIds().isEmpty())
                ? req.roomTypeIds()
                : (req.roomTypeId() == null ? List.of() : List.of(req.roomTypeId()));
        if (typeIds.isEmpty()) {
            return new InventoryRegisterResult(0, 0, 0, 0);
        }

        boolean overwrite = Boolean.TRUE.equals(req.overwrite()); // 덮어쓰기 여부

        int totalRequested = 0;     // 요청(필터 통과)된 date×roomType 수
        int affectedSum    = 0;     // INSERT+UPDATE 총합
        int skippedOutOfRange;      // 범위 밖으로 컷된 건수(추정)
        int skippedExisting;        // 기존 유지(업서트 off 시), 혹은 미반영 수

        for (Long rtId : typeIds) {
            List<LocalDate> batchDates = new ArrayList<>();
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                if (wd.isEmpty() || wd.contains(isoDow(d))) {
                    batchDates.add(d);
                    totalRequested++;
                }
            }

            // 업서트 실행 (booked는 유지, allotment/price/status만 갱신)
            int affected = registerRepository.bulkUpsert(
                    hotelId, rtId, batchDates,
                    req.price(), req.allotment(),
                    req.status() == null ? "OPEN" : req.status(),
                    overwrite
            );
            affectedSum += affected;
        }

        // outOfRange는 (원요청 범위 - today..+30 클립) 반영 + 요일 필터 고려(추정)
        LocalDate origFrom = req.from();
        LocalDate origTo   = req.to();
        LocalDate clipFrom = origFrom.isBefore(today) ? today : origFrom;
        LocalDate clipTo   = origTo.isAfter(maxDate) ? maxDate : origTo;

        int origDays = (int) Math.max(0, origTo.toEpochDay() - origFrom.toEpochDay() + 1);
        int clipDays = (int) Math.max(0, clipTo.toEpochDay() - clipFrom.toEpochDay() + 1);
        if (req.weekdays() != null && !req.weekdays().isEmpty()) {
            origDays = filteredDays(origFrom, origTo, req.weekdays());
            clipDays = filteredDays(clipFrom, clipTo, req.weekdays());
        }
        int typeCount = typeIds.size();
        skippedOutOfRange = Math.max(0, (origDays - clipDays) * typeCount);

        // INSERT+UPDATE로 반영되지 않은 나머지(= 기존 유지 등)
        skippedExisting = Math.max(0, totalRequested - affectedSum - skippedOutOfRange);

        return new InventoryRegisterResult(totalRequested, affectedSum, skippedExisting, skippedOutOfRange);
    }

    private static int isoDow(LocalDate d) {
        DayOfWeek w = d.getDayOfWeek(); // MON=1..SUN=7
        return w.getValue();
    }

    private static int filteredDays(LocalDate from, LocalDate to, List<Integer> weekdays){
        if (to.isBefore(from)) return 0;
        Set<Integer> wd = new HashSet<>(weekdays);
        int c=0; LocalDate d=from;
        while(!d.isAfter(to)){
            if (wd.contains(d.getDayOfWeek().getValue())) c++;
            d=d.plusDays(1);
        }
        return c;
    }
}
