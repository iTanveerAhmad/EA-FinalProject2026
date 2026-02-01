package miu.cs544.releasesystem.release.controller;

import miu.cs544.releasesystem.release.domain.Release;
import miu.cs544.releasesystem.release.domain.Task;
import miu.cs544.releasesystem.release.dto.ReleaseRequest;
import miu.cs544.releasesystem.release.dto.TaskRequest;
import miu.cs544.releasesystem.release.service.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ReleaseController manages the REST API for release lifecycle management.
 * Enforces workflow constraints including Sequential Task Execution and Hotfix Logic.
 */
@RestController
@RequestMapping("/releases")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    /**
     * Create a new release. Admin only.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Release> createRelease(@RequestBody ReleaseRequest request) {
        return ResponseEntity.ok(releaseService.createRelease(request));
    }

    /**
     * Get all releases in the system. Admin only.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Release>> getAllReleases() {
        return ResponseEntity.ok(releaseService.getAllReleases());
    }

    /**
     * Add a task to a release. Admin only.
     * If adding to a completed release, it triggers Hotfix Logic: the release is automatically re-opened.
     */
    @PostMapping("/{id}/tasks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Release> addTask(@PathVariable String id, @RequestBody TaskRequest taskRequest) {
        return ResponseEntity.ok(releaseService.addTaskToRelease(id, taskRequest));
    }

    /**
     * Complete a release. Admin only. All tasks must be COMPLETED.
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> completeRelease(@PathVariable String id) {
        releaseService.completeRelease(id);
        return ResponseEntity.ok().build();
    }
}
