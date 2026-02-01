package miu.cs544.releasesystem.notification.service;

import miu.cs544.releasesystem.notification.domain.NotificationLog;
import miu.cs544.releasesystem.notification.event.*;
import miu.cs544.releasesystem.notification.repository.NotificationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final EmailService emailService;
    private final NotificationLogRepository notificationLogRepository;
    private final ObjectMapper objectMapper;

    @RetryableTopic(attempts = 4, backOff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), dltTopicSuffix = "-dlt", include = Exception.class)
    @KafkaListener(topics = "task-events", groupId = "notification-group")
    public void listenTaskEvents(ConsumerRecord<String, String> record) {
        String key = record.key(); // "assigned", "completed", "hotfix", "stale"
        String value = record.value();
        
        log.info("Received Event - Key: {}, Value: {}", key, value);

        try {
            if ("assigned".equals(key)) {
                TaskAssignedEvent event = objectMapper.readValue(value, TaskAssignedEvent.class);
                sendNotification(event.getDeveloperId(), "New Task Assigned", "You have been assigned task " + event.getTaskId(), "TaskAssigned");
            } else if ("hotfix".equals(key)) {
                HotfixTaskAddedEvent event = objectMapper.readValue(value, HotfixTaskAddedEvent.class);
                sendNotification(event.getDeveloperId(), "URGENT: Hotfix Task Added", "A hotfix task '" + event.getTaskTitle() + "' has been added to your release!", "HotfixAdded");
            } else if ("stale".equals(key)) {
                StaleTaskDetectedEvent event = objectMapper.readValue(value, StaleTaskDetectedEvent.class);
                sendNotification(event.getDeveloperId(), "Stale Task Reminder", "Task " + event.getTaskId() + " has been active for " + event.getDuration(), "StaleTask");
            }
            // "completed" might not need notification
        } catch (Exception e) {
            log.error("Error processing event", e);
            throw new RuntimeException("Failed to process task event", e);
        }
    }

    @RetryableTopic(attempts = 4, backOff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), dltTopicSuffix = "-dlt", include = Exception.class)
    @KafkaListener(topics = "system-events", groupId = "notification-group")
    public void listenSystemEvents(ConsumerRecord<String, String> record) {
        try {
            SystemErrorEvent event = objectMapper.readValue(record.value(), SystemErrorEvent.class);
            sendNotification("admin@company.com", "System Error Alert", "Error: " + event.getMessage(), "SystemError");
        } catch (Exception e) {
            log.error("Error processing system event", e);
            throw new RuntimeException("Failed to process system event", e);
        }
    }

    private void sendNotification(String recipient, String subject, String body, String type) {
        // Send Email
        emailService.sendEmail(recipient, subject, body);

        // Log to DB
        NotificationLog logEntry = new NotificationLog();
        logEntry.setRecipient(recipient);
        logEntry.setSubject(subject);
        logEntry.setBody(body);
        logEntry.setEventType(type);
        logEntry.setTimestamp(Instant.now());
        notificationLogRepository.save(logEntry);
    }
}
