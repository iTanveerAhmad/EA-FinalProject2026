package miu.cs544.releasesystem.release.dto;

import lombok.Data;

@Data
public class ReplyRequest {
    private String content;
    private String developerId;
}
