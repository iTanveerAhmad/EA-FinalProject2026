package miu.cs544.releasesystem.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import miu.cs544.releasesystem.notification.domain.NotificationLog;
import miu.cs544.releasesystem.notification.event.TaskAssignedEvent;
import miu.cs544.releasesystem.notification.event.SystemErrorEvent;
import miu.cs544.releasesystem.notification.repository.NotificationLogRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaConsumerServiceTest {

    private EmailService emailService;
    private NotificationLogRepository notificationLogRepository;
    private ObjectMapper objectMapper;
    private KafkaConsumerService kafkaConsumerService;

    @BeforeEach
    void setUp() {
        emailService = Mockito.mock(EmailService.class);
        notificationLogRepository = Mockito.mock(NotificationLogRepository.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        kafkaConsumerService = new KafkaConsumerService(emailService, notificationLogRepository, objectMapper);
    }

    @Test
    void listenTaskEvents_sendsEmailAndLogsNotification_onAssignedEvent() throws Exception {
        TaskAssignedEvent event = new TaskAssignedEvent("task1", "dev1", "dev1@example.com", "rel1");
        String json = objectMapper.writeValueAsString(event);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("task-events", 0, 0L, "assigned", json);

        kafkaConsumerService.listenTaskEvents(record);

        verify(emailService).sendEmail("dev1@example.com", "New Task Assigned", "You have been assigned task task1");

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        NotificationLog logEntry = captor.getValue();

        assertThat(logEntry.getRecipient()).isEqualTo("dev1@example.com");
        assertThat(logEntry.getSubject()).contains("New Task Assigned");
        assertThat(logEntry.getDeliveryStatus()).isEqualTo("SENT");
        assertThat(logEntry.getTimestamp()).isNotNull();
    }

    @Test
    void listenTaskEvents_logsFailure_whenEmailSendThrows() throws Exception {
        TaskAssignedEvent event = new TaskAssignedEvent("task2", "dev2", "dev2@example.com", "rel1");
        String json = objectMapper.writeValueAsString(event);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("task-events", 0, 0L, "assigned", json);

        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendEmail(any(), any(), any());

        // The listener gracefully handles email failures by logging them, not throwing
        // This ensures Kafka message processing continues even if email sending fails
        kafkaConsumerService.listenTaskEvents(record);

        // Verify email was attempted
        verify(emailService).sendEmail("dev2@example.com", "New Task Assigned", "You have been assigned task task2");

        // Verify notification log was saved with FAILED status
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        NotificationLog logEntry = captor.getValue();

        assertThat(logEntry.getDeliveryStatus()).isEqualTo("FAILED");
        assertThat(logEntry.getErrorMessage()).contains("SMTP error");
    }

    @Test
    void listenSystemEvents_sendsAdminAlert() throws Exception {
        SystemErrorEvent event = new SystemErrorEvent("KAFKA_DOWN", "Kafka failure", Instant.now());
        String json = objectMapper.writeValueAsString(event);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("system-events", 0, 0L, "error", json);

        kafkaConsumerService.listenSystemEvents(record);

        verify(emailService).sendEmail("admin@company.com", "System Error Alert", "Error: Kafka failure");
        verify(notificationLogRepository).save(any(NotificationLog.class));
    }
}

