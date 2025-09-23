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

        int changed = 0;

        for (LocalDate d = cmd.from(); !d.isAfter(cmd.to()); d = d.plusDays(1)) {
            if (!matchesWeekday(d, cmd.weekdays())) continue;

            // ✅ 단일 날짜를 checkIn=d, checkOut=d+1 로 조회
            List<BookingDay> rows = repo.findForUpdate(hotelId, cmd.roomTypeId(), d, d.plusDays(1));
            BookingDay row;
            if (!rows.isEmpty()) {
                row = rows.get(0);
            } else {
                row = new BookingDay(hotelId, cmd.roomTypeId(), d);
            }

            if (cmd.allotment() != null) row.setAllotment(cmd.allotment());
            if (cmd.price() != null)     row.setPrice(cmd.price());
            if (cmd.status() != null)    row.setStatus(BookingDayStatus.valueOf(cmd.status()));

            if (row.getAllotment() < 0 || row.getPrice() < 0) {
                throw new IllegalArgumentException("allotment/price must be >= 0");
            }
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
            try { BookingDayStatus.valueOf(cmd.status()); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("status must be OPEN/CLOSED/SOLD_OUT"); }
        }
    }

    private InventoryDayDto toDto(BookingDay b) {
        return new InventoryDayDto(
                b.getStayDate(),
                b.getAllotment(),
                b.getBooked(),
                b.getPrice(),
                b.getStatus().name(),
                Optional.ofNullable(b.getRemainingQty())
                        .orElse(Math.max(b.getAllotment() - b.getBooked(), 0)),
                Optional.ofNullable(b.getSellable())
                        .orElse((b.getStatus() == BookingDayStatus.OPEN) &&
                                (b.getAllotment() - b.getBooked() > 0))
        );
    }
}
