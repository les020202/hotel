package com.example.hotelres.common.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class PagedResponse<T> {
    private List<T> items;
    private Integer limit;
    private Integer offset;
    private Long total;     // 전체 개수
    private Boolean hasMore;
    private Integer nextOffset;
}
