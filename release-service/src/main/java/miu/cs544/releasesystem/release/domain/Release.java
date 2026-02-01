package miu.cs544.releasesystem.release.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import miu.cs544.releasesystem.release.domain.ReleaseStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private ReleaseStatus status = ReleaseStatus.IN_PROGRESS;
    private List<Task> tasks = new ArrayList<>();
}
