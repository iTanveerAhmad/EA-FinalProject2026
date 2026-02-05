package miu.cs544.releasesystem.release.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import miu.cs544.releasesystem.release.domain.ReleaseStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "releases")
public class Release {
    @Id
    private String id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean reopened = false;
    private Integer hotfixCount = 0;
    private Instant reopenedAt;
    private ReleaseStatus status = ReleaseStatus.IN_PROGRESS;
    private List<Task> tasks = new ArrayList<>();
}
