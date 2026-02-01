package com.example.releasesystem.release.controller;

import com.example.releasesystem.release.domain.ChatMessage;
import com.example.releasesystem.release.domain.ChatSession;
import com.example.releasesystem.release.security.SecurityUtil;
import com.example.releasesystem.release.service.OllamaService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final OllamaService ollamaService;

    /**
     * Start a new chat session for the current developer.
     * In production, developerId is extracted from JWT.
     */
    @PostMapping("/session")
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
     * Send a message in a chat session. The Ollama service will process the message
     * and return an AI-generated response with context awareness.
     */
    @PostMapping("/{sessionId}/message")
    public ResponseEntity<ChatMessage> sendMessage(
            @PathVariable String sessionId, 
            @RequestBody ChatRequest request) {
        
        return ResponseEntity.ok(ollamaService.sendMessage(sessionId, request.getMessage()));
    }

    /**
     * Get chat history for a session.
     */
    @GetMapping("/{sessionId}/history")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(ollamaService.getHistory(sessionId));
    }

    @Data
    public static class ChatRequest {
        private String message;
    }
}
