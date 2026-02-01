package miu.cs544.releasesystem.release.service;

import miu.cs544.releasesystem.release.domain.ChatMessage;
import miu.cs544.releasesystem.release.domain.ChatSession;
import miu.cs544.releasesystem.release.dto.OllamaRequest;
import miu.cs544.releasesystem.release.dto.OllamaResponse;
import miu.cs544.releasesystem.release.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {

    private final ChatSessionRepository chatSessionRepository;
    private final WebClient.Builder webClientBuilder;

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "llama3"; // Or "mistral", "llama2", etc.

    public ChatSession createSession(String developerId) {
        ChatSession session = new ChatSession();
        session.setDeveloperId(developerId);
        return chatSessionRepository.save(session);
    }

    public ChatMessage sendMessage(String sessionId, String userMessage) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // 1. Save User Message
        session.getMessages().add(new ChatMessage("user", userMessage, Instant.now()));
        chatSessionRepository.save(session);

        // 2. Build Context (Last 5 messages)
        String context = buildContext(session.getMessages());
        String fullPrompt = context + "\nUser: " + userMessage + "\nAssistant:";

        // 3. Call Ollama
        String aiResponseText;
        try {
            OllamaRequest request = new OllamaRequest();
            request.setModel(MODEL);
            request.setPrompt(fullPrompt);
            request.setStream(false);

            OllamaResponse response = webClientBuilder.build()
                    .post()
                    .uri(OLLAMA_URL)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .block(); // Blocking for simplicity in this sync method

            aiResponseText = response != null ? response.getResponse() : "Error: No response from AI.";
        } catch (Exception e) {
            log.error("Ollama connection failed", e);
            aiResponseText = "I'm sorry, I cannot connect to my brain (Ollama) right now. Is it running?";
        }

        // 4. Save AI Response
        ChatMessage aiMessage = new ChatMessage("assistant", aiResponseText, Instant.now());
        session.getMessages().add(aiMessage);
        chatSessionRepository.save(session);

        return aiMessage;
    }

    private String buildContext(List<ChatMessage> messages) {
        int start = Math.max(0, messages.size() - 6); // Take last 5 history + current (which isn't in list yet technically, but we added it)
        // actually we added it before calling this.
        
        return messages.stream()
                .skip(Math.max(0, messages.size() - 5)) // Last 5
                .map(m -> (m.getRole().equals("user") ? "User: " : "Assistant: ") + m.getContent())
                .collect(Collectors.joining("\n"));
    }
    
    public List<ChatMessage> getHistory(String sessionId) {
         return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"))
                .getMessages();
    }
}
