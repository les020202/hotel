// src/main/java/com/example/hotelres/hotel/Hotel.java
package com.example.hotelres.hotel;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hotels")
@Data
@NoArgsConstructor 
@AllArgsConstructor
@Builder

public class Hotel {
	
  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length=100, nullable=false) 
  private String name;
  
  @Column(length=50)  
  private String region;
  
  @Column(length=200) 
  private String address;
  
  @Column(precision=2, scale=1) // DECIMAL(2,1)
  private BigDecimal rating;

  @Column(name = "grade_level", length = 20)
  private Integer gradeLevel;

  private String phone;

  @Column(name = "homepage_url")
  private String homepageUrl;

  @Column(precision = 10, scale = 7)
  private BigDecimal latitude;
  
  @Column(precision = 10, scale = 7)
  private BigDecimal longitude;

  @Column(name = "canonical_key", unique = true)
  private String canonicalKey;
  
  @Column(name = "official_grade")
  private String officialGrade;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "cover_image_type")
  private CoverImageType coverImageType;
  
  @Column(length=500) 
  private String coverImageUrl;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "cover_image_template")
  private CoverImageTemplate coverImageTemplate;
  
  public enum CoverImageType { NONE,UPLOADED,TEMPLATE }
  public enum CoverImageTemplate { DEFAULT, BEACH, CITY } // 실제 값에 맞춰 수정

  // getter/setter …
}
