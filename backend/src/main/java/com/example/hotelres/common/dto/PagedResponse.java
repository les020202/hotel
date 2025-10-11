package com.example.hotelres.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 오프셋 기반 페이지네이션 응답
 * - SearchService 등에서 사용 (limit/offset/hasMore/nextOffset)
 */
@Data
@Builder
public class PagedResponse<T> {
    private List<T> items;      // 콘텐츠
    private Integer limit;      // pageSize
    private Integer offset;     // off
    private Long total;         // 전체 개수
    private Boolean hasMore;    // 다음 페이지 존재 여부
    private Integer nextOffset; // 다음 off (없으면 null)
}
