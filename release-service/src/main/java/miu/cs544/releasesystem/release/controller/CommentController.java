package miu.cs544.releasesystem.release.controller;

import miu.cs544.releasesystem.release.dto.ReplyRequest;
import miu.cs544.releasesystem.release.security.SecurityUtil;
import miu.cs544.releasesystem.release.service.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final ReleaseService releaseService;

    /**
     * Reply to a comment (supports nested threading - Reddit-style). Developer/Admin only.
     */
    @PostMapping("/{id}/reply")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<Void> replyToComment(
            @PathVariable String id,
            @RequestBody ReplyRequest request) {
        String developerId = request.getDeveloperId();
        if (developerId == null || developerId.isBlank()) {
            try {
                developerId = SecurityUtil.getCurrentUsername();
            } catch (IllegalStateException e) {
                return ResponseEntity.status(401).build();
            }
        }
        releaseService.addReplyToComment(id, developerId, request.getContent());
        return ResponseEntity.ok().build();
    }
}
