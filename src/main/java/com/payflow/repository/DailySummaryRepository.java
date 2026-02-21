package com.payflow.repository;

import com.payflow.model.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {

    Optional<DailySummary> findByAccountIdAndSummaryDate(Long accountId, LocalDate summaryDate);

    List<DailySummary> findByAccountIdAndSummaryDateBetweenOrderBySummaryDateAsc(
            Long accountId, LocalDate from, LocalDate to);
}
