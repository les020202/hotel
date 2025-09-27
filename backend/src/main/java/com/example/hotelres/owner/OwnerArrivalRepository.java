package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.ArrivalItemDto;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OwnerArrivalRepository extends JpaRepository<BookingEntity, Long> {

    @Query("""
    select new com.example.hotelres.owner.dto.ArrivalItemDto(
      b.id, bi.id,
      '',                         
      b.guests,
      rt.id, rt.typeCode, rt.name,
      b.checkIn, b.checkOut, b.nights,
      case when exists (
        select a.id from RoomNightAssignmentEntity a
        where a.bookingItemId = bi.id
          and a.releasedAt is null
      ) then true else false end
    )
    from BookingEntity b
      join BookingItemEntity bi on bi.booking = b
      join RoomTypeEntity rt on rt.id = bi.roomTypeId
    where b.hotelId = :hotelId
      and b.status = com.example.hotelres.owner.BookingStatus.CONFIRMED
      and b.checkIn = :date
    order by b.createdAt desc
  """)
    List<ArrivalItemDto> findArrivals(@Param("hotelId") Long hotelId,
                                      @Param("date") LocalDate date);
}
