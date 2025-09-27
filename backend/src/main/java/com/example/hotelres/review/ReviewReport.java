// src/main/java/com/example/hotelres/review/ReviewReport.java
package com.example.hotelres.review;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_reports",
        uniqueConstraints = @UniqueConstraint(name="uk_rr_one_reporter",
                columnNames = {"review_id","reporter_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewReport {

    public enum ReporterRole { USER, OWNER }
    public enum Status { PENDING, RESOLVED }
    public enum Action { KEEP, HIDE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="review_id", nullable=false)
    private Long reviewId;

    @Column(name="reporter_id", nullable=false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name="reporter_role", nullable=false, length=10)
    private ReporterRole reporterRole;

    @Column(name="reason", nullable=false, length=50)
    private String reason;

    @Column(name="detail", length=1000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false, length=10)
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name="action", length=10)
    private Action action;

    @Column(name="admin_comment", length=1000)
    private String adminComment;

    @Column(name="handled_by_admin_id")
    private Long handledByAdminId;

    @Column(name="handled_at")
    private LocalDateTime handledAt;

    @Column(name="created_at", insertable=false, updatable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", insertable=false, updatable=false)
    private LocalDateTime updatedAt;
}
