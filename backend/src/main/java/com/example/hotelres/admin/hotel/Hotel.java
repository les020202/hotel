package com.example.hotelres.admin.hotel;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hotels")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Hotel {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 50)
  private String region;

  @Column(length = 200)
  private String address;

  @Column(length = 50)
  private String phone;

  @Column(precision=2, scale=1) // DECIMAL(2,1)
  private BigDecimal rating;

  private Integer gradeLevel;

  @Column(length=500) 
  private String coverImageUrl;

  @Column(name = "homepage_url", length = 300)
  private String homepageUrl;

  @Column(name = "canonical_key", length = 64, unique = true)
  private String canonicalKey;
}
