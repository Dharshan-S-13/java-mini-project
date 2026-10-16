package com.skycast.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycast.model.ChatRequest;
import com.skycast.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class OllamaService {
    private final ObjectMapper objectMapper;

    public OllamaService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChatResponse chat(ChatRequest request) {
        // Placeholder, migrate Ollama client logic in Task 2
        return new ChatResponse("Ollama service not yet migrated.");
    }
}
