package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.RoomStatusDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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

    /** 객실 현황 조회 */
    @Transactional(readOnly = true)
    public List<RoomStatusDto> getStatus(Long hotelId, LocalDate date) {
        // 소유자 검증
        guard.assertOwnerOfHotel(hotelId);

        LocalDate target = (date != null) ? date : LocalDate.now();
        return repo.findStatus(hotelId, target);
    }

    /** 하우스키핑 상태 변경 */
    @Transactional
    public void updateHk(Long hotelId, Long roomId, HousekeepingStatus hk) {
        // 소유자 검증
        guard.assertOwnerOfHotel(hotelId);

        RoomEntity r = roomRepo.findById(roomId).orElseThrow();
        if (!r.getHotelId().equals(hotelId)) {
            throw new IllegalArgumentException("room mismatch");
        }
        r.setHousekeeping(hk);
        // JPA dirty checking 또는 명시 저장
        roomRepo.save(r);
    }

    /** 객실 운영 상태 변경 */
    @Transactional
    public void updateStatus(Long hotelId, Long roomId, RoomStatus st) {
        // 소유자 검증
        guard.assertOwnerOfHotel(hotelId);

        RoomEntity r = roomRepo.findById(roomId).orElseThrow();
        if (!r.getHotelId().equals(hotelId)) {
            throw new IllegalArgumentException("room mismatch");
        }
        r.setStatus(st);
        roomRepo.save(r);
    }
}
