package miu.cs544.releasesystem.notification.service;

import miu.cs544.releasesystem.notification.domain.NotificationLog;
import miu.cs544.releasesystem.notification.event.*;
import miu.cs544.releasesystem.notification.repository.NotificationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class KafkaConsumerService {

    private final EmailService emailService;
    private final NotificationLogRepository notificationLogRepository;
    private final ObjectMapper objectMapper;

    public KafkaConsumerService(EmailService emailService,
                                NotificationLogRepository notificationLogRepository,
                                ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.notificationLogRepository = notificationLogRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "task-events", groupId = "notification-group")
    public void listenTaskEvents(ConsumerRecord<String, String> record) {
        String key = record.key(); // "assigned", "completed", "hotfix", "stale"
        String value = record.value();
        
        log.info("Received Event - Key: {}, Value: {}", key, value);

        try {
            if ("assigned".equals(key)) {
                TaskAssignedEvent event = objectMapper.readValue(value, TaskAssignedEvent.class);
                String recipient = resolveRecipient(event.getDeveloperEmail(), event.getDeveloperId());
                log.info("TaskAssigned - developerId={}, developerEmail={}, resolved recipient={}", 
                    event.getDeveloperId(), event.getDeveloperEmail(), recipient);
                sendNotification(recipient, "New Task Assigned", "You have been assigned task " + event.getTaskId(), "TaskAssigned");
            } else if ("hotfix".equals(key)) {
                HotfixTaskAddedEvent event = objectMapper.readValue(value, HotfixTaskAddedEvent.class);
                String recipient = resolveRecipient(event.getDeveloperEmail(), event.getDeveloperId());
                sendNotification(recipient, "URGENT: Hotfix Task Added", "A hotfix task '" + event.getTaskTitle() + "' has been added to your release!", "HotfixAdded");
            } else if ("stale".equals(key)) {
                StaleTaskDetectedEvent event = objectMapper.readValue(value, StaleTaskDetectedEvent.class);
                String recipient = resolveRecipient(event.getDeveloperEmail(), event.getDeveloperId());
                sendNotification(recipient, "Stale Task Reminder", "Task " + event.getTaskId() + " has been active for " + event.getDuration(), "StaleTask");
            }
            // "completed" might not need notification
        } catch (Exception e) {
            log.error("Error processing event", e);
            throw new RuntimeException("Failed to process task event", e);
        }
    }

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

    private String resolveRecipient(String developerEmail, String developerId) {
        if (developerEmail != null && !developerEmail.isBlank()) {
            return developerEmail;
        }
        return developerId; // fallback: EmailService will append default-domain if needed
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
