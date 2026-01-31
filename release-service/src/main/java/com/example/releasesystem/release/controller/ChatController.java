package com.example.releasesystem.release.controller;

import com.example.releasesystem.release.domain.ChatMessage;
import com.example.releasesystem.release.domain.ChatSession;
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

    @PostMapping("/session")
    public ResponseEntity<ChatSession> startSession(@RequestParam String developerId) {
        return ResponseEntity.ok(ollamaService.createSession(developerId));
    }

    @PostMapping("/{sessionId}/message")
    public ResponseEntity<ChatMessage> sendMessage(@PathVariable String sessionId, @RequestBody ChatRequest request) {
        return ResponseEntity.ok(ollamaService.sendMessage(sessionId, request.getMessage()));
    }

    @GetMapping("/{sessionId}/history")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(ollamaService.getHistory(sessionId));
    }

    @Data
    public static class ChatRequest {
        private String message;
    }
}
