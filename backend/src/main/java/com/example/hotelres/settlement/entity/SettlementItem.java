// entity/SettlementItem.java
package com.example.hotelres.settlement.entity;

import com.example.hotelres.settlement.entity.enums.ServiceType;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "settlement_items")
public class SettlementItem {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
  private Long id;
  @Column(name="statement_id", nullable=false) private Long statementId;
  @Column(name="booking_id", nullable=false) private Long bookingId;
  @Column(name="booking_item_id", nullable=false) private Long bookingItemId;
  @Column(name="hotel_id", nullable=false) private Long hotelId;
  @Column(name="room_type_id") private Long roomTypeId; // nullable
  @Column(name="checkout_date", nullable=false) private LocalDate checkoutDate;
  @Enumerated(EnumType.STRING) @Column(name="service_type", nullable=false)
  private ServiceType serviceType = ServiceType.ROOM_NIGHT;
  @Column(name="gross_amount", nullable=false) private Long grossAmount;
  @Column(name="discount_amount", nullable=false) private Long discountAmount;
  @Column(name="platform_fee_amount", nullable=false) private Long platformFeeAmount;
  @Column(name="net_to_hotel", nullable=false) private Long netToHotel;

  public SettlementItem() {}
  public SettlementItem(Long statementId, Long bookingId, Long bookingItemId, Long hotelId, Long roomTypeId,
                        LocalDate checkoutDate, ServiceType serviceType,
                        Long gross, Long discount, Long fee, Long net) {
    this.statementId=statementId; this.bookingId=bookingId; this.bookingItemId=bookingItemId;
    this.hotelId=hotelId; this.roomTypeId=roomTypeId; this.checkoutDate=checkoutDate;
    this.serviceType=serviceType; this.grossAmount=gross; this.discountAmount=discount;
    this.platformFeeAmount=fee; this.netToHotel=net;
  }
}
