package com.payflow.service;

import com.payflow.model.DailySummary;
import com.payflow.repository.DailySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailySummaryService {

    private final DailySummaryRepository dailySummaryRepository;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public List<DailySummary> getSummaries(Long accountId, LocalDate from, LocalDate to, Long userId) {
        accountService.getById(accountId, userId);
        return dailySummaryRepository.findByAccountIdAndSummaryDateBetweenOrderBySummaryDateAsc(accountId, from, to);
    }
}
