// src/main/java/com/example/hotelres/owner/OwnerAssignmentService.java
package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.AvailableRoomDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor
public class OwnerAssignmentService {
    private final HotelOwnerGuard guard;
    private final BookingItemRepository itemRepo;
    private final BookingRepository bookingRepo;         // 앞서 만든 BookingRepository
    private final RoomRepository roomRepo;
    private final RoomTypeRepository roomTypeRepo;
    private final RoomNightAssignmentRepository assignRepo;

    public List<AvailableRoomDto> findAvailableRooms(String loginId, Long hotelId,
                                                     LocalDate from, LocalDate to,
                                                     int minCapacity, String requestedType, boolean upgrade) {
        guard.checkAccess(loginId, hotelId);
        return roomRepo.toDtos(
                roomRepo.findAvailableRoomsNative(hotelId, from, to, minCapacity, requestedType, upgrade)
        );
    }

    @Transactional
    public int assign(String loginId, Long hotelId, Long bookingItemId, Long roomId) {
        guard.checkAccess(loginId, hotelId);

        var item = itemRepo.findById(bookingItemId).orElseThrow();
        var booking = bookingRepo.findById(item.getBookingId()).orElseThrow();
        if (!booking.getHotelId().equals(hotelId)) throw new IllegalArgumentException("다른 호텔 예약");

        var room = roomRepo.findById(roomId).orElseThrow();
        if (!room.getHotelId().equals(hotelId)) throw new IllegalArgumentException("다른 호텔 방");

        // 등급/업그레이드 허용
        var reqRt = roomTypeRepo.findById(item.getRoomTypeId()).orElseThrow();
        var actRt = roomTypeRepo.findById(room.getRoomTypeId()).orElseThrow();
        String req = reqRt.getTypeCode();
        String act = actRt.getTypeCode();
        if (!isTypeAllowed(req, act)) throw new IllegalStateException("등급 불가 (업그레이드만 허용)");

        if (room.getCapacity()!=null && room.getCapacity() < booking.getGuests())
            throw new IllegalStateException("인원 초과");

        int n = 0;
        for (LocalDate d = booking.getCheckIn(); d.isBefore(booking.getCheckOut()); d = d.plusDays(1)) {
            // 충돌 잠금
            if (assignRepo.lockRoomDate(roomId, d) != null) throw new IllegalStateException("해당 날짜 이미 배정된 방");
            if (assignRepo.lockItemDate(item.getId(), d) != null) throw new IllegalStateException("해당 아이템은 이미 배정됨");

            assignRepo.save(new RoomNightAssignmentEntity(roomId, item.getId(), d));
            n++;
        }
        // 하우스키핑 표식(선택)
        room.setHousekeeping(HousekeepingStatus.OCCUPIED);
        roomRepo.save(room);
        return n;
    }

    @Transactional
    public int unassignAll(String loginId, Long hotelId, Long bookingItemId) {
        guard.checkAccess(loginId, hotelId);
        // hotel cross-check는 booking 통해 추가 확인 가능
        return assignRepo.deleteAllByBookingItemId(bookingItemId);
    }

    private boolean isTypeAllowed(String requested, String actual) {
        return rank(actual) >= rank(requested);
    }
    private int rank(String t) {
        return switch (t) {
            case "STANDARD" -> 1;
            case "DELUXE" -> 2;
            case "PREMIUM" -> 3;
            case "SUITE" -> 4;
            default -> 0;
        };
    }
}
