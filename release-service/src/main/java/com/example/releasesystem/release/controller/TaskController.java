package com.example.releasesystem.release.controller;

import com.example.releasesystem.release.domain.Task;
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

    @GetMapping("/my")
    public ResponseEntity<List<Task>> getMyTasks(@RequestParam String developerId) {
        // In a real app, developerId comes from JWT context. For now, passing as param.
        return ResponseEntity.ok(releaseService.getTasksForDeveloper(developerId));
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<Void> startTask(@PathVariable String id, @RequestParam String developerId) {
        releaseService.startTask(id, developerId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> completeTask(@PathVariable String id, @RequestParam String developerId) {
        releaseService.completeTask(id, developerId);
        return ResponseEntity.ok().build();
    }
}
