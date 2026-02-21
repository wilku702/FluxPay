package com.payflow.event;

import com.payflow.event.TransactionEvent.EventType;
import com.payflow.event.TransactionEvent.TransactionEventType;
import com.payflow.model.DailySummary;
import com.payflow.repository.DailySummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailySummaryConsumerTest {

    @Mock
    private DailySummaryRepository dailySummaryRepository;

    @InjectMocks
    private DailySummaryConsumer consumer;

    @Test
    void creditEventCreatesNewSummary() {
        LocalDateTime now = LocalDateTime.of(2024, 6, 15, 10, 30);
        TransactionEvent event = new TransactionEvent(
                1L, 42L, EventType.DEPOSIT, TransactionEventType.CREDIT,
                BigDecimal.valueOf(500), BigDecimal.valueOf(1500), null, now
        );

        when(dailySummaryRepository.findByAccountIdAndSummaryDate(42L, LocalDate.of(2024, 6, 15)))
                .thenReturn(Optional.empty());
        when(dailySummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<DailySummary> captor = ArgumentCaptor.forClass(DailySummary.class);
        verify(dailySummaryRepository).save(captor.capture());

        DailySummary saved = captor.getValue();
        assertThat(saved.getAccountId()).isEqualTo(42L);
        assertThat(saved.getSummaryDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(saved.getTotalCredits()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(saved.getTotalDebits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getTransactionCount()).isEqualTo(1);
        assertThat(saved.getClosingBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }

    @Test
    void debitEventUpdatesExistingSummary() {
        LocalDateTime now = LocalDateTime.of(2024, 6, 15, 14, 0);
        TransactionEvent event = new TransactionEvent(
                2L, 42L, EventType.WITHDRAWAL, TransactionEventType.DEBIT,
                BigDecimal.valueOf(200), BigDecimal.valueOf(800), null, now
        );

        DailySummary existing = new DailySummary(42L, LocalDate.of(2024, 6, 15));
        existing.setTotalCredits(BigDecimal.valueOf(500));
        existing.setTransactionCount(1);
        existing.setClosingBalance(BigDecimal.valueOf(1000));

        when(dailySummaryRepository.findByAccountIdAndSummaryDate(42L, LocalDate.of(2024, 6, 15)))
                .thenReturn(Optional.of(existing));
        when(dailySummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<DailySummary> captor = ArgumentCaptor.forClass(DailySummary.class);
        verify(dailySummaryRepository).save(captor.capture());

        DailySummary saved = captor.getValue();
        assertThat(saved.getTotalDebits()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(saved.getTransactionCount()).isEqualTo(2);
        assertThat(saved.getClosingBalance()).isEqualByComparingTo(BigDecimal.valueOf(800));
    }

    @Test
    void multipleEventsOnSameDayAggregateCorrectly() {
        LocalDate date = LocalDate.of(2024, 6, 15);

        DailySummary summary = new DailySummary(42L, date);
        summary.setTotalCredits(BigDecimal.valueOf(500));
        summary.setTotalDebits(BigDecimal.valueOf(100));
        summary.setTransactionCount(2);

        TransactionEvent event = new TransactionEvent(
                3L, 42L, EventType.DEPOSIT, TransactionEventType.CREDIT,
                BigDecimal.valueOf(300), BigDecimal.valueOf(1700), null,
                date.atTime(16, 0)
        );

        when(dailySummaryRepository.findByAccountIdAndSummaryDate(42L, date))
                .thenReturn(Optional.of(summary));
        when(dailySummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<DailySummary> captor = ArgumentCaptor.forClass(DailySummary.class);
        verify(dailySummaryRepository).save(captor.capture());

        DailySummary saved = captor.getValue();
        assertThat(saved.getTotalCredits()).isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(saved.getTransactionCount()).isEqualTo(3);
    }
}
