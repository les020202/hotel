package com.example.hotelres.owner.occupancy;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RoomTypeOccupancyService {

  private final RoomTypeOccupancyRepository repo;

  public RoomTypeOccupancyService(RoomTypeOccupancyRepository repo) {
    this.repo = repo;
  }

  public List<RoomTypeOccupancyRow> getRoomTypeOccupancy(Long hotelId, LocalDate from, LocalDate to) {
    return repo.findRoomTypeOccupancy(hotelId, from, to);
  }
}
