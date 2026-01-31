package com.example.releasesystem.release.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private String content;
    private String developerId; // In real app, from SecurityContext
}
