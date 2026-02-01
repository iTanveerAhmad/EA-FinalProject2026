package com.example.releasesystem.release.controller;

import com.example.releasesystem.release.domain.Task;
import com.example.releasesystem.release.security.SecurityUtil;
import com.example.releasesystem.release.service.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final ReleaseService releaseService;

    /**
     * Get tasks assigned to the current authenticated developer.
     * In production, developerId is extracted from JWT. For backward compatibility,
     * can also be passed as @RequestParam.
     */
    @GetMapping("/my")
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
     * Start a task. Only the assigned developer can start their tasks.
     * Single In-Process Rule is enforced: only ONE task per developer can be in-progress at any time.
     * Sequential Task Execution: cannot start task N until task N-1 is complete.
     */
    @PatchMapping("/{id}/start")
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
     * Complete a task. Only the assigned developer can complete their tasks.
     */
    @PatchMapping("/{id}/complete")
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
}
