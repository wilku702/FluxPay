package com.payflow.controller;

import com.payflow.model.DailySummary;
import com.payflow.repository.DailySummaryRepository;
import com.payflow.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountId}/summaries")
@RequiredArgsConstructor
public class DailySummaryController {

    private final DailySummaryRepository dailySummaryRepository;
    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<DailySummary>> getSummaries(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        // Verify ownership
        accountService.getById(accountId, userId);

        List<DailySummary> summaries = dailySummaryRepository
                .findByAccountIdAndSummaryDateBetweenOrderBySummaryDateAsc(accountId, from, to);
        return ResponseEntity.ok(summaries);
    }
}
