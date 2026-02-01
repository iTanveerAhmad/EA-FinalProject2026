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
public class Comment {
    private String id = UUID.randomUUID().toString();
    private String authorId;
    private String content;
    private Instant timestamp = Instant.now();
    private List<Comment> replies = new ArrayList<>();
}
