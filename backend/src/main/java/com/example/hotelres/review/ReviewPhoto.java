package com.example.hotelres.review;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_photos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewPhoto {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(nullable = false, length = 300)
    private String url;
}
