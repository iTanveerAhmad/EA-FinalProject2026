package com.example.releasesystem.release.dto;

import lombok.Data;

@Data
public class TaskRequest {
    private String title;
    private String description;
    private String assignedDeveloperId;
    private Integer orderIndex;
}
