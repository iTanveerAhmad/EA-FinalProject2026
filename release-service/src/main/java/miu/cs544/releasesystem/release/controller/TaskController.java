package miu.cs544.releasesystem.release.controller;

import miu.cs544.releasesystem.release.domain.Comment;
import miu.cs544.releasesystem.release.domain.Task;
import miu.cs544.releasesystem.release.dto.CommentRequest;
import miu.cs544.releasesystem.release.security.SecurityUtil;
import miu.cs544.releasesystem.release.service.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final ReleaseService releaseService;

    /**
     * Get tasks assigned to the current authenticated developer.
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<List<Task>> getMyTasks(
            @RequestParam(required = false) String developerId) {
        
        // Extract from JWT if not provided as parameter
        if (developerId == null || developerId.isBlank()) {
            try {
                developerId = SecurityUtil.getCurrentUsername();
            } catch (IllegalStateException e) {
                return ResponseEntity.status(401).build();
            }
        }
        
        return ResponseEntity.ok(releaseService.getTasksForDeveloper(developerId));
    }

    /**
     * Start a task. Developer/Admin only. Single In-Process and Sequential rules enforced.
     */
    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<Void> startTask(
            @PathVariable String id, 
            @RequestParam(required = false) String developerId) {
        
        // Extract from JWT if not provided as parameter
        if (developerId == null || developerId.isBlank()) {
            try {
                developerId = SecurityUtil.getCurrentUsername();
            } catch (IllegalStateException e) {
                return ResponseEntity.status(401).build();
            }
        }
        
        releaseService.startTask(id, developerId);
        return ResponseEntity.ok().build();
    }

    /**
     * Complete a task. Developer/Admin only.
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<Void> completeTask(
            @PathVariable String id, 
            @RequestParam(required = false) String developerId) {
        
        // Extract from JWT if not provided as parameter
        if (developerId == null || developerId.isBlank()) {
            try {
                developerId = SecurityUtil.getCurrentUsername();
            } catch (IllegalStateException e) {
                return ResponseEntity.status(401).build();
            }
        }
        
        releaseService.completeTask(id, developerId);
        return ResponseEntity.ok().build();
    }

    /**
     * Add a comment to a task (forum/discussion board). Developer/Admin only.
     */
    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<Void> addComment(
            @PathVariable String id,
            @RequestBody CommentRequest request) {
        String developerId = request.getDeveloperId();
        if (developerId == null || developerId.isBlank()) {
            try {
                developerId = SecurityUtil.getCurrentUsername();
            } catch (IllegalStateException e) {
                return ResponseEntity.status(401).build();
            }
        }
        releaseService.addComment(id, developerId, request.getContent());
        return ResponseEntity.ok().build();
    }

    /**
     * Get all comments for a task (threaded discussion). Developer/Admin only.
     */
    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<List<Comment>> getComments(@PathVariable String id) {
        return ResponseEntity.ok(releaseService.getCommentsForTask(id));
    }
}
