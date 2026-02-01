package miu.cs544.releasesystem.release.controller;

import miu.cs544.releasesystem.release.domain.ChatMessage;
import miu.cs544.releasesystem.release.domain.ChatSession;
import miu.cs544.releasesystem.release.security.SecurityUtil;
import miu.cs544.releasesystem.release.service.OllamaService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final OllamaService ollamaService;

    /**
     * Start a new chat session for the current developer.
     */
    @PostMapping("/session")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<ChatSession> startSession(
            @RequestParam(required = false) String developerId) {
        
        // Extract from JWT if not provided as parameter
        if (developerId == null || developerId.isBlank()) {
            try {
                developerId = SecurityUtil.getCurrentUsername();
            } catch (IllegalStateException e) {
                return ResponseEntity.status(401).build();
            }
        }
        
        return ResponseEntity.ok(ollamaService.createSession(developerId));
    }

    /**
     * Send a message in a chat session. Ollama returns AI response with context.
     */
    @PostMapping("/{sessionId}/message")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<ChatMessage> sendMessage(
            @PathVariable String sessionId, 
            @RequestBody ChatRequest request) {
        
        return ResponseEntity.ok(ollamaService.sendMessage(sessionId, request.getMessage()));
    }

    /**
     * Get chat history for a session.
     */
    @GetMapping("/{sessionId}/history")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(ollamaService.getHistory(sessionId));
    }

    @Data
    public static class ChatRequest {
        private String message;
    }
}
