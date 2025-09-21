package com.example.hotelres.owner.dto;

/**
 * Native Query Projection (SELECT alias ↔ getter명 일치)
 */
public interface OwnerHotelView {
    Long getId();
    String getName();
    String getRegion();
    Integer getGradeLevel(); // hotels.grade_level
    String getBusinessNo();  // hotel_owners.business_no
}
