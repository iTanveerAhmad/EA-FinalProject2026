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
<<<<<<< HEAD
    private Instant startedAt; // Added for Stale detection
=======
    // Audit fields and timestamps
    private Instant createdAt = Instant.now();
    private Instant startedAt;   // Used for stale detection
>>>>>>> f22b2c7 (some test cases are fixed)
    private Instant completedAt;
    private List<Comment> comments = new ArrayList<>();
}
