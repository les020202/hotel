// src/main/java/com/example/hotelres/owner/OwnerRoomsService.java
package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.RoomStatusDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OwnerRoomsService {

    private final OwnerGuard guard;
    private final OwnerRoomsRepository repo;
    private final RoomRepository roomRepo;
    private final RoomNightAssignmentRepository rnaRepo;

    public OwnerRoomsService(OwnerGuard guard, OwnerRoomsRepository repo,
                             RoomRepository roomRepo, RoomNightAssignmentRepository rnaRepo) {
        this.guard = guard;
        this.repo = repo;
        this.roomRepo = roomRepo;
        this.rnaRepo = rnaRepo;
    }

    @Transactional(readOnly = true)
    public List<RoomStatusDto> getStatus(Long hotelId, LocalDate date) {
        guard.assertOwnerOfHotel(hotelId);
        LocalDate target = (date != null) ? date : LocalDate.now();

        List<RoomStatusDto> rows = repo.findStatus(hotelId, target);

        var guestRows = repo.findGuestByRoomOnDate(hotelId, target);
        Map<Long, OwnerRoomsRepository.RoomGuestProjection> guestMap =
                guestRows.stream().collect(Collectors.toMap(
                        OwnerRoomsRepository.RoomGuestProjection::getRoomId, g -> g));

        for (RoomStatusDto dto : rows) {
            var g = guestMap.get(dto.getId());
            if (g != null) {
                dto.setGuestName(g.getGuestName());
                dto.setGuestPhone(g.getGuestPhone());
            }
        }
        return rows;
    }

    @Transactional
    public void updateHk(Long hotelId, Long roomId, HousekeepingStatus hk) {
        guard.assertOwnerOfHotel(hotelId);
        RoomEntity r = roomRepo.findById(roomId).orElseThrow();
        if (!r.getHotelId().equals(hotelId)) throw new IllegalArgumentException("room mismatch");

        r.setHousekeeping(hk);

        if (hk == HousekeepingStatus.CLEAN && r.getStatus() != RoomStatus.ACTIVE) {
            r.setStatus(RoomStatus.ACTIVE);
        } else if ((hk == HousekeepingStatus.DIRTY || hk == HousekeepingStatus.INSPECTED)
                && r.getStatus() != RoomStatus.INACTIVE) {
            r.setStatus(RoomStatus.INACTIVE);
        }
        roomRepo.save(r);
    }

    /** 상태 변경: INACTIVE면 해당 date의 점유를 soft-release + HK 동기화 */
    @Transactional
    public void updateStatus(Long hotelId, Long roomId, RoomStatus st, LocalDate date) {
        guard.assertOwnerOfHotel(hotelId);
        LocalDate target = (date != null) ? date : LocalDate.now();

        RoomEntity r = roomRepo.findById(roomId).orElseThrow();
        if (!r.getHotelId().equals(hotelId)) throw new IllegalArgumentException("room mismatch");

        if (st == RoomStatus.INACTIVE) {
            // 🔁 활성 배정 개수 확인 후 있으면 release
            int activeCnt = rnaRepo.countActiveOnDate(roomId, target);
            if (activeCnt > 0) {
                rnaRepo.releaseByDate(roomId, target);
            }
        }

        r.setStatus(st);

        // 상태→HK 최소 동기화
        if (st == RoomStatus.ACTIVE && r.getHousekeeping() != HousekeepingStatus.CLEAN) {
            r.setHousekeeping(HousekeepingStatus.CLEAN);
        }
        if (st == RoomStatus.INACTIVE
            && r.getHousekeeping() != HousekeepingStatus.DIRTY
            && r.getHousekeeping() != HousekeepingStatus.OCCUPIED) {
            r.setHousekeeping(HousekeepingStatus.DIRTY);
        }

        roomRepo.save(r);
    }
}
