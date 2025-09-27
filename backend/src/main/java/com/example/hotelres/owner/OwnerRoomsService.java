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

    public OwnerRoomsService(OwnerGuard guard, OwnerRoomsRepository repo, RoomRepository roomRepo) {
        this.guard = guard;
        this.repo = repo;
        this.roomRepo = roomRepo;
    }

    @Transactional(readOnly = true)
    public List<RoomStatusDto> getStatus(Long hotelId, LocalDate date) {
        guard.assertOwnerOfHotel(hotelId);
        LocalDate target = (date != null) ? date : LocalDate.now();

        // 1) 기본 현황
        List<RoomStatusDto> rows = repo.findStatus(hotelId, target);

        // 2) 방별 대표 투숙객
        var guestRows = repo.findGuestByRoomOnDate(hotelId, target);
        Map<Long, OwnerRoomsRepository.RoomGuestProjection> guestMap =
                guestRows.stream().collect(Collectors.toMap(
                        OwnerRoomsRepository.RoomGuestProjection::getRoomId,
                        g -> g
                ));

        // 3) 머지
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
        roomRepo.save(r);
    }

    @Transactional
    public void updateStatus(Long hotelId, Long roomId, RoomStatus st) {
        guard.assertOwnerOfHotel(hotelId);
        RoomEntity r = roomRepo.findById(roomId).orElseThrow();
        if (!r.getHotelId().equals(hotelId)) throw new IllegalArgumentException("room mismatch");
        r.setStatus(st);
        roomRepo.save(r);
    }
}
