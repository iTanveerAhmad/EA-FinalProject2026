package miu.cs544.releasesystem.release.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private String id = UUID.randomUUID().toString();
    private String title;
    private String description;
    private TaskStatus status = TaskStatus.TODO;
    private String assignedDeveloperId;
    private Integer orderIndex;
    private Instant createdAt = Instant.now();
    private Instant startedAt;   // Used for stale detection
    private Instant completedAt;
    private List<Comment> comments = new ArrayList<>();
}
