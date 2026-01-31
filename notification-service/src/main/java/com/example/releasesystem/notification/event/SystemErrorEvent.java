package com.example.releasesystem.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemErrorEvent {
    private String errorCode;
    private String message;
    private Instant timestamp;
}
