package com.payflow.event;

import com.payflow.config.KafkaConfig;
import com.payflow.model.DailySummary;
import com.payflow.repository.DailySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailySummaryConsumer {

    private final DailySummaryRepository dailySummaryRepository;

    @KafkaListener(topics = KafkaConfig.TRANSACTION_EVENTS_TOPIC, groupId = "fluxpay-summary")
    public void consume(TransactionEvent event) {
        LocalDate eventDate = event.timestamp().toLocalDate();

        DailySummary summary = dailySummaryRepository
                .findByAccountIdAndSummaryDate(event.accountId(), eventDate)
                .orElseGet(() -> new DailySummary(event.accountId(), eventDate));

        switch (event.transactionType()) {
            case CREDIT -> summary.setTotalCredits(
                    summary.getTotalCredits().add(event.amount()));
            case DEBIT -> summary.setTotalDebits(
                    summary.getTotalDebits().add(event.amount()));
        }

        summary.setTransactionCount(summary.getTransactionCount() + 1);
        summary.setClosingBalance(event.balanceAfter());

        dailySummaryRepository.save(summary);

        log.debug("Updated daily summary for account {} on {}: credits={}, debits={}, count={}",
                event.accountId(), eventDate, summary.getTotalCredits(),
                summary.getTotalDebits(), summary.getTransactionCount());
    }
}
