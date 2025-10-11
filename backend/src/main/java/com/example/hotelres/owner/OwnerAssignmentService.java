// src/main/java/com/example/hotelres/owner/OwnerAssignmentService.java
package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.AvailableRoomDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerAssignmentService {

    private final HotelOwnerGuard guard;
    private final BookingItemRepository itemRepo;
    private final BookingRepository bookingRepo;
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

        // 업그레이드 허용 규칙
        var reqRt = roomTypeRepo.findById(item.getRoomTypeId()).orElseThrow();
        var actRt = roomTypeRepo.findById(room.getRoomTypeId()).orElseThrow();
        if (rank(actRt.getTypeCode()) < rank(reqRt.getTypeCode()))
            throw new IllegalStateException("등급 불가 (업그레이드만 허용)");

        // ✅ 여기 수정: guests는 primitive int 라서 null 비교 금지
        Integer capacity = room.getCapacity(); // nullable
        int guests = booking.getGuests();      // primitive
        if (capacity != null && capacity < guests)
            throw new IllegalStateException("인원 초과");

        // 1) 이 아이템의 '활성' 배정 전부 해제(soft release)
        assignRepo.releaseAllByBookingItemId(item.getId());

        // 2) 각 숙박일 처리
        int n = 0;
        for (LocalDate d = booking.getCheckIn(); d.isBefore(booking.getCheckOut()); d = d.plusDays(1)) {

            // 2-1) 이 아이템+해당일에 과거(해제 포함) 행이 있으면 그 행을 재활성화 하며 room_id 갱신
            Long oldId = assignRepo.findAnyIdByItemAndDateForUpdate(item.getId(), d);
            if (oldId != null) {
                assignRepo.reactivateByIdSetRoom(oldId, roomId);
                n++;
                continue;
            }

            // 2-2) 없으면, 대상 방-날짜에 "활성" 충돌이 없는지 잠금 확인
            if (assignRepo.lockRoomDate(roomId, d) != null)
                throw new IllegalStateException("해당 날짜 이미 배정된 방");

            // 2-3) 신규 행 생성
            assignRepo.save(new RoomNightAssignmentEntity(roomId, item.getId(), d));
            n++;
        }

        // 하우스키핑 표식(선택): 배정 즉시 OCCUPIED
        room.setHousekeeping(HousekeepingStatus.OCCUPIED);
        roomRepo.save(room);

        return n;
    }

    /** 컨트롤러에서 호출하는 전체 해제 API */
    @Transactional
    public int unassignAll(String loginId, Long hotelId, Long bookingItemId) {
        guard.checkAccess(loginId, hotelId);

        var item = itemRepo.findById(bookingItemId).orElseThrow();
        var booking = bookingRepo.findById(item.getBookingId()).orElseThrow();
        if (!booking.getHotelId().equals(hotelId))
            throw new IllegalArgumentException("다른 호텔 예약");

        return assignRepo.releaseAllByBookingItemId(bookingItemId);
    }

    private int rank(String t) {
        return switch (t) {
            case "STANDARD" -> 1;
            case "DELUXE"   -> 2;
            case "PREMIUM"  -> 3;
            case "SUITE"    -> 4;
            default -> 0;
        };
    }
}
