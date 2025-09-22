package com.example.hotelres.owner;

import jakarta.persistence.*;

@Entity
@Table(name = "hotel_owners",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hotel_user_login", columnNames = {"hotel_id","user_login_id"}),
                @UniqueConstraint(name = "uk_hotel_business",   columnNames = {"hotel_id","business_no"})
        },
        indexes = {
                @Index(name = "ix_ho_user_login", columnList = "user_login_id"),
                @Index(name = "ix_ho_biz",        columnList = "business_no")
        }
)
public class HotelOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "user_login_id", length = 20, nullable = false)
    private String userLoginId;

    @Column(name = "business_no", length = 10, nullable = false)
    private String businessNo;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }

    public String getUserLoginId() { return userLoginId; }
    public void setUserLoginId(String userLoginId) { this.userLoginId = userLoginId; }

    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
}
