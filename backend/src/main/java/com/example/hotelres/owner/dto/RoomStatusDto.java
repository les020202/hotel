package com.example.hotelres.owner.dto;

import com.example.hotelres.owner.HousekeepingStatus;
import com.example.hotelres.owner.RoomStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoomStatusDto {

    private Long id;

    // roomNo 를 문자열로 보관하면 양쪽 케이스(String/Integer) 모두 커버하기 쉽습니다.
    private String roomNumber;

    private Integer floor;
    private Long roomTypeId;
    private String roomTypeCode;
    private String roomTypeName;
    private Integer capacity;
    private RoomStatus status;
    private HousekeepingStatus housekeeping;
    private boolean occupied;

    // 추가로 나중에 Service에서 채워주는 필드
    private String guestName;
    private String guestPhone;

    // ✅ (A) roomNo 가 Integer 인 경우와 매칭되는 생성자
    public RoomStatusDto(
            Long id,
            Integer roomNo,              // ← Integer
            Integer floor,
            Long roomTypeId,
            String roomTypeCode,
            String roomTypeName,
            Integer capacity,
            RoomStatus status,
            HousekeepingStatus housekeeping,
            boolean occupied
    ) {
        this.id = id;
        this.roomNumber = (roomNo == null) ? null : String.valueOf(roomNo);
        this.floor = floor;
        this.roomTypeId = roomTypeId;
        this.roomTypeCode = roomTypeCode;
        this.roomTypeName = roomTypeName;
        this.capacity = capacity;
        this.status = status;
        this.housekeeping = housekeeping;
        this.occupied = occupied;
    }

    // ✅ (B) roomNo 가 String 인 경우와 매칭되는 생성자
    public RoomStatusDto(
            Long id,
            String roomNo,               // ← String
            Integer floor,
            Long roomTypeId,
            String roomTypeCode,
            String roomTypeName,
            Integer capacity,
            RoomStatus status,
            HousekeepingStatus housekeeping,
            boolean occupied
    ) {
        this.id = id;
        this.roomNumber = roomNo;
        this.floor = floor;
        this.roomTypeId = roomTypeId;
        this.roomTypeCode = roomTypeCode;
        this.roomTypeName = roomTypeName;
        this.capacity = capacity;
        this.status = status;
        this.housekeeping = housekeeping;
        this.occupied = occupied;
    }
}
