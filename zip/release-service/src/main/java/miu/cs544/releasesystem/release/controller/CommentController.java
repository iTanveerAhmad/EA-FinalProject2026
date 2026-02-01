package miu.cs544.releasesystem.release.controller;

import miu.cs544.releasesystem.release.dto.CommentRequest;
import miu.cs544.releasesystem.release.security.SecurityUtil;
import miu.cs544.releasesystem.release.service.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final ReleaseService releaseService;

    /**
     * Add a comment to a task. Comments support collaborative notes on task progress.
     * In production, developerId is extracted from JWT.
     */
    @PostMapping("/tasks/{taskId}")
    public ResponseEntity<Void> addComment(
            @PathVariable String taskId, 
            @RequestBody CommentRequest request) {
        
        String developerId = request.getDeveloperId();
        
        // Extract from JWT if not provided
        if (developerId == null || developerId.isBlank()) {
            try {
                developerId = SecurityUtil.getCurrentUsername();
            } catch (IllegalStateException e) {
                return ResponseEntity.status(401).build();
            }
        }
        
        releaseService.addComment(taskId, developerId, request.getContent());
        return ResponseEntity.ok().build();
    }
}
