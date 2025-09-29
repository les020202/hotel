package com.example.hotelres.owner.occupancy;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/owner/hotels/{hotelId}/analytics")
public class RoomTypeOccupancyController {

  private final RoomTypeOccupancyService service;

  public RoomTypeOccupancyController(RoomTypeOccupancyService service) {
    this.service = service;
  }

  @GetMapping("/room-type-occupancy")
  public List<RoomTypeOccupancyRow> roomTypeOccupancy(
      @PathVariable Long hotelId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
  ) {
    return service.getRoomTypeOccupancy(hotelId, from, to);
  }
}
