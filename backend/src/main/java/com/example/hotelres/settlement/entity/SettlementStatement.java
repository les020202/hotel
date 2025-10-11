// src/main/java/com/example/hotelres/settlement/entity/SettlementStatement.java
package com.example.hotelres.settlement.entity;

import com.example.hotelres.settlement.entity.enums.StatementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "settlement_statements",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_stmt_hotel_period",
            columnNames = {"hotel_id", "period_start", "period_end"}
        )
    },
    indexes = {
        @Index(name = "idx_stmt_hotel_period", columnList = "hotel_id, period_start, period_end"),
        @Index(name = "idx_stmt_status_period", columnList = "status, period_start, period_end"),
        @Index(name = "idx_stmt_created_at", columnList = "created_at")
    }
)
@Getter
@NoArgsConstructor
public class SettlementStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="hotel_id", nullable=false)
    private Long hotelId;

    @Column(name="period_start", nullable=false)
    private LocalDate periodStart;

    @Column(name="period_end", nullable=false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length = 20)
    private StatementStatus status = StatementStatus.PLANNED;

    @Column(name="payable_amount", nullable=false)
    private Long payableAmount = 0L;

    // 정산 계좌 스냅샷
    @Column(name="payout_bank_code", length=20)
    private String payoutBankCode;

    @Column(name="payout_account_no", length=64)
    private String payoutAccountNo;

    @Column(name="payout_holder_name", length=100)
    private String payoutHolderName;

    @Column(name="created_at", nullable=false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name="settled_at")
    private LocalDateTime settledAt;

    // 낙관적 락(옵션) - 동시 정산확정 충돌 방지
    @Version
    @Column(name="version")
    private Long version;

    // 편의 생성자
    public SettlementStatement(Long hotelId, LocalDate start, LocalDate end,
                               String bankCode, String accountNo, String holderName) {
        this.hotelId = hotelId;
        this.periodStart = start;
        this.periodEnd   = end;
        this.status = StatementStatus.PLANNED;
        this.payableAmount = 0L;
        this.payoutBankCode   = bankCode;
        this.payoutAccountNo  = accountNo;
        this.payoutHolderName = holderName;
    }

    /* ===== 라이프사이클 ===== */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null)    this.status = StatementStatus.PLANNED;
        if (this.payableAmount == null) this.payableAmount = 0L;
    }

    /* ===== 변경용 최소 setter ===== */
    public void setPayoutBankCode(String v)   { this.payoutBankCode = v; }
    public void setPayoutAccountNo(String v)  { this.payoutAccountNo = v; }
    public void setPayoutHolderName(String v) { this.payoutHolderName = v; }
    public void setPayableAmount(Long v)      { this.payableAmount = (v == null ? 0L : v); }
    public void setStatus(StatementStatus s)  { this.status = s; }
    public void setSettledAt(LocalDateTime t) { this.settledAt = t; }
}
