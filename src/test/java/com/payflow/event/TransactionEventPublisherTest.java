package com.payflow.event;

import com.payflow.config.KafkaConfig;
import com.payflow.event.TransactionEvent.EventType;
import com.payflow.event.TransactionEvent.TransactionEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionEventPublisherTest {

    @Mock
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @InjectMocks
    private TransactionEventPublisher publisher;

    @Test
    void publishSendsEventWithAccountIdAsKey() {
        TransactionEvent event = new TransactionEvent(
                1L, 42L, EventType.DEPOSIT, TransactionEventType.CREDIT,
                BigDecimal.valueOf(100), BigDecimal.valueOf(1100),
                null, LocalDateTime.now()
        );

        when(kafkaTemplate.send(eq(KafkaConfig.TRANSACTION_EVENTS_TOPIC), eq("42"), eq(event)))
                .thenReturn(new CompletableFuture<>());

        publisher.publish(event);

        verify(kafkaTemplate).send(KafkaConfig.TRANSACTION_EVENTS_TOPIC, "42", event);
    }

    @Test
    void publishSendsTransferEventWithCorrelationId() {
        UUID correlationId = UUID.randomUUID();
        TransactionEvent event = new TransactionEvent(
                2L, 10L, EventType.TRANSFER_DEBIT, TransactionEventType.DEBIT,
                BigDecimal.valueOf(250), BigDecimal.valueOf(750),
                correlationId, LocalDateTime.now()
        );

        when(kafkaTemplate.send(eq(KafkaConfig.TRANSACTION_EVENTS_TOPIC), eq("10"), eq(event)))
                .thenReturn(new CompletableFuture<>());

        publisher.publish(event);

        verify(kafkaTemplate).send(KafkaConfig.TRANSACTION_EVENTS_TOPIC, "10", event);
    }
}
