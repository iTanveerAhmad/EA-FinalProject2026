package miu.cs544.releasesystem.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_logs")
public class NotificationLog {
    @Id
    private String id;
    private String recipient;
    private String subject;
    private String body;
    private String eventType;
    private Instant timestamp;

    // Delivery/audit metadata
    private String deliveryStatus;   // e.g. SENT, FAILED
    private String relatedEventId;   // optional Kafka key or id
    private String errorMessage;     // populated on failure
}
