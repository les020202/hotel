package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.RoomStatusDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OwnerRoomsRepository extends JpaRepository<RoomEntity, Long> {

    @Query("""
    select new com.example.hotelres.owner.dto.RoomStatusDto(
      r.id, r.roomNo, r.floor,
      rt.id, rt.typeCode, rt.name,
      r.capacity, r.status, r.housekeeping,
      case when a.id is not null then true else false end
    )
    from RoomEntity r
      join RoomTypeEntity rt on rt.id = r.roomTypeId
      left join RoomNightAssignmentEntity a
        on a.roomId = r.id and a.stayDate = :date
    where r.hotelId = :hotelId
    order by r.floor asc, r.roomNo asc
  """)
    List<RoomStatusDto> findStatus(@Param("hotelId") Long hotelId,
                                   @Param("date") LocalDate date);
}
