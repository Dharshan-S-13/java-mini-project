package com.skycast.model;

import java.util.List;

public record ChatRequest(String message, String city, List<ChatMessage> history) {
}
