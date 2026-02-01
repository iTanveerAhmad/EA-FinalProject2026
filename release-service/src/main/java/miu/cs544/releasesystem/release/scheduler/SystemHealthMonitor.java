package miu.cs544.releasesystem.release.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import miu.cs544.releasesystem.release.event.SystemErrorEvent;
import miu.cs544.releasesystem.release.service.KafkaProducerService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Monitors system health (MongoDB, Kafka) and publishes SystemErrorEvent when failures are detected.
 * Notification Service consumes these events and alerts admin via email.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class SystemHealthMonitor {

    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProducerService kafkaProducerService;

    @Scheduled(fixedRate = 60000) // Every minute
    public void checkHealth() {
        checkMongoDb();
        checkKafka();
    }

    private void checkMongoDb() {
        try {
            mongoTemplate.getDb().runCommand(org.bson.Document.parse("{ ping: 1 }"));
        } catch (Exception e) {
            log.error("MongoDB health check failed", e);
            try {
                kafkaProducerService.sendSystemErrorEvent(new SystemErrorEvent(
                    "MONGODB_DOWN",
                    "Database connection failure: " + e.getMessage(),
                    Instant.now()
                ));
            } catch (Exception ex) {
                log.warn("Could not send MongoDB failure alert", ex);
            }
        }
    }

    private void checkKafka() {
        try {
            kafkaTemplate.partitionsFor("task-events");
        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            try {
                kafkaProducerService.sendSystemErrorEvent(new SystemErrorEvent(
                    "KAFKA_DOWN",
                    "Kafka connection failure: " + e.getMessage(),
                    Instant.now()
                ));
            } catch (Exception ex) {
                log.warn("Could not send Kafka failure alert (Kafka may be down)", ex);
            }
        }
    }
}
