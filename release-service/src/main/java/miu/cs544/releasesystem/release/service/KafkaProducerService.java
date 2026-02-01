package miu.cs544.releasesystem.release.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendTaskAssignedEvent(Object event) {
        kafkaTemplate.send("task-events", "assigned", event);
        log.info("Sent TaskAssignedEvent: {}", event);
    }

    public void sendTaskCompletedEvent(Object event) {
        kafkaTemplate.send("task-events", "completed", event);
        log.info("Sent TaskCompletedEvent: {}", event);
    }

    public void sendHotfixTaskAddedEvent(Object event) {
        kafkaTemplate.send("task-events", "hotfix", event);
        log.info("Sent HotfixTaskAddedEvent: {}", event);
    }

    public void sendStaleTaskDetectedEvent(Object event) {
        kafkaTemplate.send("task-events", "stale", event);
        log.info("Sent StaleTaskDetectedEvent: {}", event);
    }

    public void sendSystemErrorEvent(Object event) {
        kafkaTemplate.send("system-events", "error", event);
        log.info("Sent SystemErrorEvent: {}", event);
    }
}
