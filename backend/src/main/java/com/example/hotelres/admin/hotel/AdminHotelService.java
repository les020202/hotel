package com.example.hotelres.admin.hotel;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminHotelService {

    private final HotelRepository hotelRepo;
    private final JdbcTemplate jdbc;   // ✅ 자식 테이블 네이티브 삭제용

    /* 목록 */
    @Transactional(readOnly = true)
    public Page<HotelDtos.ListItem> list(String region, String q, int page, int size){
        var pageable = PageRequest.of(Math.max(page,0), Math.max(size,1));
        return hotelRepo.search(region, q, pageable).map(HotelDtos.ListItem::from);
    }

    /* 상세 */
    @Transactional(readOnly = true)
    public HotelDtos.Detail detail(Long id){
        var h = hotelRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return HotelDtos.Detail.from(h);
    }

    /* 수정 */
    @Transactional
    public void update(Long id, HotelDtos.UpdateReq req){
        var h = hotelRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (req.name() != null)       h.setName(req.name());
        if (req.region() != null)     h.setRegion(req.region());
        if (req.address() != null)    h.setAddress(req.address());
        if (req.phone() != null)      h.setPhone(req.phone());
        if (req.homepageUrl() != null)h.setHomepageUrl(req.homepageUrl());

        hotelRepo.save(h);
    }

    /* 삭제: 예약 있으면 409, 없으면 자식부터 정리 후 호텔 삭제 */
    @Transactional
    public void delete(Long id){
        if (!hotelRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // 1) 예약 존재 여부 (있으면 409)
        Integer hasBooking = jdbc.queryForObject(
            "SELECT CASE WHEN COUNT(*)>0 THEN 1 ELSE 0 END FROM bookings WHERE hotel_id=?",
            Integer.class, id
        );
        if (hasBooking != null && hasBooking == 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "해당 호텔에 예약 내역이 있어 삭제할 수 없습니다.");
        }

        // 2) 자식 테이블 정리 (참조 순서 주의)
        jdbc.update("DELETE FROM booking_day      WHERE hotel_id=?", id); // booking_day -> room_types/hotel
        jdbc.update("""
            DELETE r
                FROM rooms r
                JOIN room_types rt ON rt.id = r.room_type_id
                WHERE rt.hotel_id = ?
        """, id);                                                          // rooms -> room_types
        jdbc.update("DELETE FROM room_types       WHERE hotel_id=?", id);   // room_types -> hotel
        jdbc.update("DELETE FROM rate_plans       WHERE hotel_id=?", id);   // rate_plans -> hotel
        jdbc.update("DELETE FROM hotel_amenities  WHERE hotel_id=?", id);   // amenities -> hotel (있으면)
        jdbc.update("DELETE FROM hotel_owners     WHERE hotel_id=?", id);   // owners -> hotel

        // 3) 마지막으로 호텔 삭제
        try {
            hotelRepo.deleteById(id);
        } catch (EmptyResultDataAccessException ignore) {
            // 이미 없어도 조용히 무시
        }
    }
}
