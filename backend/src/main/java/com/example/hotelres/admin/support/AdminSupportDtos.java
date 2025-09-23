package com.example.hotelres.admin.support;

import com.example.hotelres.support.Notice;
import java.time.LocalDateTime;

/** 공지 DTO들 */
public final class AdminSupportDtos {

    private AdminSupportDtos() {}

    /** 공지사항 DTO */
    public record NoticeDto(
            Long id,
            String title,
            String content,
            boolean pinned,
            Long authorId,
            String authorName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static NoticeDto from(Notice n) {
            Long aid = n.getAuthor() != null ? n.getAuthor().getId() : null;
            String aname = n.getAuthor() != null ? n.getAuthor().getName() : null;
            return new NoticeDto(
                    n.getId(),
                    n.getTitle(),
                    n.getContent(),
                    n.isPinned(),
                    aid,
                    aname,
                    n.getCreatedAt(),
                    n.getUpdatedAt()
            );
        }
    }
}
