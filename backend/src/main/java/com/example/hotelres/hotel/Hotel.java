// src/main/java/com/example/hotelres/hotel/Hotel.java
package com.example.hotelres.hotel;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "hotels")
public class Hotel {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length=100, nullable=false) private String name;
  @Column(length=50)  private String region;
  @Column(length=200) private String address;

  @Column(precision=2, scale=1) // DECIMAL(2,1)
  private BigDecimal rating;

  private Integer gradeLevel;

  @Column(length=500) private String coverImageUrl;

  // getter/setter …
}
