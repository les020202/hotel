package com.example.hotelres.admin.support;

import com.example.hotelres.support.Faq;
import java.time.LocalDateTime;

/** FAQ DTO */
public final class FaqAdminDtos {

    private FaqAdminDtos() {}

    public record FaqDto(
            Long id,
            String category,
            String question,
            String answer,
            Long authorId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static FaqDto from(Faq f) {
            return new FaqDto(
                    f.getId(),
                    f.getCategory(),
                    f.getQuestion(),
                    f.getAnswer(),
                    f.getAuthorId(),
                    f.getCreatedAt(),
                    f.getUpdatedAt()
            );
        }
    }
}
