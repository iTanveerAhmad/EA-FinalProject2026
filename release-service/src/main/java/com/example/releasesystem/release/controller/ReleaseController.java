package com.example.releasesystem.release.controller;

import com.example.releasesystem.release.domain.Release;
import com.example.releasesystem.release.domain.Task;
import com.example.releasesystem.release.dto.ReleaseRequest;
import com.example.releasesystem.release.dto.TaskRequest;
import com.example.releasesystem.release.service.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/releases")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @PostMapping
    public ResponseEntity<Release> createRelease(@RequestBody ReleaseRequest request) {
        return ResponseEntity.ok(releaseService.createRelease(request));
    }

    @GetMapping
    public ResponseEntity<List<Release>> getAllReleases() {
        return ResponseEntity.ok(releaseService.getAllReleases());
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<Release> addTask(@PathVariable String id, @RequestBody TaskRequest taskRequest) {
        return ResponseEntity.ok(releaseService.addTaskToRelease(id, taskRequest));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> completeRelease(@PathVariable String id) {
        releaseService.completeRelease(id);
        return ResponseEntity.ok().build();
    }
}
