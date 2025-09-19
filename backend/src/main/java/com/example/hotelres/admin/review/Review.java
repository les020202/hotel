// src/main/java/.../admin/review/Review.java
package com.example.hotelres.admin.review;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Review {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
}
