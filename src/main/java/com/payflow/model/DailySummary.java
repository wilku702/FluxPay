package com.payflow.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_account_summaries")
@Getter
@Setter
@NoArgsConstructor
public class DailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "total_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name = "total_debits", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDebits = BigDecimal.ZERO;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount = 0;

    @Column(name = "closing_balance", precision = 19, scale = 4)
    private BigDecimal closingBalance;

    public DailySummary(Long accountId, LocalDate summaryDate) {
        this.accountId = accountId;
        this.summaryDate = summaryDate;
    }
}
