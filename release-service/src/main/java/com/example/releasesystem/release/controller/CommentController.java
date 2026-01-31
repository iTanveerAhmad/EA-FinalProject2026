package com.example.releasesystem.release.controller;

import com.example.releasesystem.release.dto.CommentRequest;
import com.example.releasesystem.release.service.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments") // Requirements say /tasks/{id}/comments, I'll add that to TaskController or here
@RequiredArgsConstructor
public class CommentController {

    private final ReleaseService releaseService;

    // Based on PDF: POST /tasks/{id}/comments
    @PostMapping("/tasks/{taskId}")
    public ResponseEntity<Void> addComment(@PathVariable String taskId, @RequestBody CommentRequest request) {
        releaseService.addComment(taskId, request.getDeveloperId(), request.getContent());
        return ResponseEntity.ok().build();
    }
}
