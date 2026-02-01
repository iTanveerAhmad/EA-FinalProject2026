package miu.cs544.releasesystem.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaleTaskDetectedEvent {
    private String taskId;
    private String developerId;
    private String duration;
}
