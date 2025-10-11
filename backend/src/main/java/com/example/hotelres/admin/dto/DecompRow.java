package com.example.hotelres.admin.dto;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DecompRow {
    private String key;
    private String name;
    private long   gmv;    // 서비스에서 getGmv() 호출
    private double share;
}
