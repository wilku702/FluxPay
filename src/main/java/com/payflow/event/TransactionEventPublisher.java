package com.payflow.event;

import com.payflow.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public void publish(TransactionEvent event) {
        String key = String.valueOf(event.accountId());
        kafkaTemplate.send(KafkaConfig.TRANSACTION_EVENTS_TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction event for account {}: {}",
                                event.accountId(), ex.getMessage());
                    } else {
                        log.debug("Published {} event for account {}, partition {}",
                                event.eventType(), event.accountId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
