package com.skycast.controller;

import com.skycast.model.ChatRequest;
import com.skycast.model.ChatResponse;
import com.skycast.service.OllamaService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final OllamaService ollamaService;

    public ChatController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return ollamaService.chat(request);
    }
}
