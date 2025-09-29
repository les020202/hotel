package com.example.hotelres.owner.occupancy;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class RoomTypeOccupancyRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public RoomTypeOccupancyRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<RoomTypeOccupancyRow> findRoomTypeOccupancy(Long hotelId, LocalDate from, LocalDate to) {
    String sql = """
      SELECT
        bd.room_type_id AS roomTypeId,
        rt.name         AS roomTypeName,
        SUM(bd.booked)  AS bookedNights,
        SUM(bd.allotment) AS allotmentNights,
        ROUND(SUM(bd.booked) / NULLIF(SUM(bd.allotment), 0) * 100, 1) AS occupancyRate
      FROM booking_day bd
      JOIN room_types rt ON rt.id = bd.room_type_id
      WHERE bd.hotel_id = :hotelId
        AND bd.stay_date BETWEEN :from AND :to
        AND bd.is_sellable = 1
        AND bd.status = 'OPEN'
      GROUP BY bd.room_type_id, rt.name
      ORDER BY occupancyRate DESC, roomTypeName ASC
      """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("hotelId", hotelId)
        .addValue("from", java.sql.Date.valueOf(from))
        .addValue("to", java.sql.Date.valueOf(to));

    return jdbc.query(sql, params, (rs, i) -> new RoomTypeOccupancyRow(
        rs.getLong("roomTypeId"),
        rs.getString("roomTypeName"),
        rs.getLong("bookedNights"),
        rs.getLong("allotmentNights"),
        rs.getDouble("occupancyRate")
    ));
  }
}
