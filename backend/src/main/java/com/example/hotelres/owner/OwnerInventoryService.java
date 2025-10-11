package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.InventoryBulkCommand;
import com.example.hotelres.owner.dto.InventoryDayDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerInventoryService {

    private final HotelOwnerGuard guard;
    private final BookingDayRepository repo;

    @Transactional(readOnly = true)
    public List<InventoryDayDto> getInventory(String loginId, Long hotelId, Long roomTypeId,
                                              LocalDate from, LocalDate to) {
        guard.checkAccess(loginId, hotelId);
        List<BookingDay> list = repo.findRange(hotelId, roomTypeId, from, to);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public int bulkUpdate(String loginId, Long hotelId, InventoryBulkCommand cmd) {
        guard.checkAccess(loginId, hotelId);
        validateCmd(cmd);

        final LocalDate today   = LocalDate.now();
        final LocalDate maxDate = today.plusDays(30);

        int changed = 0;

        for (LocalDate d = cmd.from(); !d.isAfter(cmd.to()); d = d.plusDays(1)) {
            // 범위 밖(지난 날짜 / +30일 초과)은 적용하지 않음
            if (d.isBefore(today) || d.isAfter(maxDate)) continue;
            if (!matchesWeekday(d, cmd.weekdays())) continue;

            // 단일 날짜를 checkIn=d, checkOut=d+1 로 조회
            List<BookingDay> rows = repo.findForUpdate(hotelId, cmd.roomTypeId(), d, d.plusDays(1));
            boolean isNew = rows.isEmpty();
            BookingDay row = isNew ? new BookingDay(hotelId, cmd.roomTypeId(), d) : rows.get(0);

            // ✅ 신규 생성이면 allotment는 필수(1 이상)
            if (isNew) {
                if (cmd.allotment() == null || cmd.allotment() <= 0) {
                    throw new IllegalArgumentException("allotment is required (>0) for a new date");
                }
                row.setAllotment(cmd.allotment());
            } else {
                // 기존 행이면 전달된 값만 갱신
                if (cmd.allotment() != null) row.setAllotment(cmd.allotment());
            }

            if (cmd.price()  != null) row.setPrice(cmd.price());

            if (cmd.status() != null) {
                // SOLD_OUT은 수동 입력 금지 (자동 파생). OPEN/CLOSED만 수용.
                if ("OPEN".equalsIgnoreCase(cmd.status()) || "CLOSED".equalsIgnoreCase(cmd.status())) {
                    row.setStatus(BookingDayStatus.valueOf(cmd.status()));
                }
            }

            // 공통 검증
            if (row.getAllotment() < 0 || row.getPrice() < 0) {
                throw new IllegalArgumentException("allotment/price must be >= 0");
            }
            // 기존 예약 건수보다 총배정을 낮출 수 없음
            if (row.getBooked() > row.getAllotment()) {
                throw new IllegalStateException("booked cannot exceed allotment");
            }

            repo.save(row);
            changed++;
        }

        return changed;
    }

    private boolean matchesWeekday(LocalDate d, Set<Integer> weekdays) {
        if (weekdays == null || weekdays.isEmpty()) return true;
        int iso = d.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
        return weekdays.contains(iso);
    }

    private void validateCmd(InventoryBulkCommand cmd) {
        if (cmd.roomTypeId() == null) throw new IllegalArgumentException("roomTypeId required");
        if (cmd.from() == null || cmd.to() == null) throw new IllegalArgumentException("from/to required");
        if (cmd.to().isBefore(cmd.from())) throw new IllegalArgumentException("to before from");
        if (cmd.status() != null) {
            // SOLD_OUT은 자동 파생이므로 입력 금지
            if (!"OPEN".equalsIgnoreCase(cmd.status()) && !"CLOSED".equalsIgnoreCase(cmd.status())) {
                throw new IllegalArgumentException("status must be OPEN/CLOSED (SOLD_OUT is derived)");
            }
        }
    }

    private InventoryDayDto toDto(BookingDay b) {
        int allot     = Optional.ofNullable(b.getAllotment()).orElse(0);
        int booked    = Optional.ofNullable(b.getBooked()).orElse(0);
        int remaining = Math.max(allot - booked, 0);

        String persisted = (b.getStatus() == null) ? "OPEN" : b.getStatus().name();
        // 파생 상태 계산: CLOSED 우선, 아니면 재고로 SOLD_OUT/OPEN 판정
        String status = "CLOSED".equalsIgnoreCase(persisted)
                ? "CLOSED"
                : (remaining == 0 ? "SOLD_OUT" : "OPEN");

        boolean sellable = "OPEN".equalsIgnoreCase(persisted) && remaining > 0;

        return new InventoryDayDto(
                b.getStayDate(),
                allot,
                booked,
                Optional.ofNullable(b.getPrice()).orElse(0),
                status,
                remaining,
                sellable
        );
    }
}
