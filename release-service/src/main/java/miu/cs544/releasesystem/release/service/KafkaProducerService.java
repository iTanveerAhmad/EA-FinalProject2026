package miu.cs544.releasesystem.release.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    private void recordKafkaMetric(String topic, String key) {
        meterRegistry.counter("kafka_events_total", "topic", topic, "key", key).increment();
    }

    public void sendTaskAssignedEvent(Object event) {
        String topic = "task-events";
        String key = "assigned";
        kafkaTemplate.send(topic, key, event);
        recordKafkaMetric(topic, key);
        log.info("Sent TaskAssignedEvent: {}", event);
    }

    public void sendTaskCompletedEvent(Object event) {
        String topic = "task-events";
        String key = "completed";
        kafkaTemplate.send(topic, key, event);
        recordKafkaMetric(topic, key);
        log.info("Sent TaskCompletedEvent: {}", event);
    }

    public void sendHotfixTaskAddedEvent(Object event) {
        String topic = "task-events";
        String key = "hotfix";
        kafkaTemplate.send(topic, key, event);
        recordKafkaMetric(topic, key);
        log.info("Sent HotfixTaskAddedEvent: {}", event);
    }

    public void sendStaleTaskDetectedEvent(Object event) {
        String topic = "task-events";
        String key = "stale";
        kafkaTemplate.send(topic, key, event);
        recordKafkaMetric(topic, key);
        log.info("Sent StaleTaskDetectedEvent: {}", event);
    }

    public void sendSystemErrorEvent(Object event) {
        String topic = "system-events";
        String key = "error";
        kafkaTemplate.send(topic, key, event);
        recordKafkaMetric(topic, key);
        log.info("Sent SystemErrorEvent: {}", event);
    }
}
