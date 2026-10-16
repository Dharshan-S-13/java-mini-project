package com.skycast.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycast.model.ChatMessage;
import com.skycast.model.ChatRequest;
import com.skycast.model.ChatResponse;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OllamaService {
    private static final String SYSTEM_PROMPT =
            "You are SkyCast's helpful weather assistant. Use only supplied verified weather data for current conditions and forecasts. "
                    + "Never invent weather information. If verified weather data is absent, say you cannot verify current conditions.";

    private final WeatherService weatherService;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String configuredModel;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public OllamaService(
            WeatherService weatherService,
            ObjectMapper objectMapper,
            @Value("${ollama.url:http://127.0.0.1:11434}") String baseUrl,
            @Value("${ollama.model:${OLLAMA_MODEL:qwen3.5:latest}}") String configuredModel) {
        this.weatherService = weatherService;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.configuredModel = configuredModel;
    }

    public ChatResponse chat(ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new WeatherApiException(HttpStatus.BAD_REQUEST, "Enter a message to chat.");
        }
        if (request.message().length() > 4000) {
            throw new WeatherApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Chat message is too long.");
        }

        String context = "";
        if (request.city() != null && !request.city().isBlank()) {
            try {
                context = "\n\n" + weatherService.getWeatherContext(request.city());
            } catch (WeatherApiException exception) {
                context = "\n\nVerified weather data could not be retrieved: "
                        + exception.getMessage()
                        + " Do not state current weather conditions or forecasts as fact.";
            }
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        if (request.history() != null) {
            request.history().stream().skip(Math.max(0, request.history().size() - 12))
                    .filter(turn -> validHistoryMessage(turn))
                    .forEach(turn -> messages.add(Map.of(
                            "role", turn.role(), "content", turn.content())));
        }
        messages.add(Map.of("role", "user", "content", request.message() + context));
        Map<String, Object> payload = Map.of(
                "model", configuredModel,
                "stream", false,
                "think", false,
                "options", Map.of("num_predict", 512),
                "messages", messages);
        try {
            String requestJson = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .timeout(Duration.ofMinutes(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 404) {
                throw new WeatherApiException(
                        HttpStatus.BAD_GATEWAY,
                        "Ollama could not find model " + configuredModel + ". Install it with ollama pull " + configuredModel + ".");
            }
            if (response.statusCode() != 200) {
                throw new WeatherApiException(
                        HttpStatus.BAD_GATEWAY, "Ollama returned an error (HTTP " + response.statusCode() + ").");
            }
            JsonNode result = objectMapper.readTree(response.body()).path("message").path("content");
            if (!result.isTextual() || result.asText().isBlank()) {
                throw new WeatherApiException(
                        HttpStatus.BAD_GATEWAY, "Ollama returned an empty response.");
            }
            return new ChatResponse(result.asText().trim());
        } catch (HttpTimeoutException exception) {
            throw new WeatherApiException(
                    HttpStatus.GATEWAY_TIMEOUT, "Ollama took too long to respond. Try again.");
        } catch (ConnectException exception) {
            throw new WeatherApiException(
                    HttpStatus.BAD_GATEWAY, "Ollama is unavailable at " + baseUrl + ". Start Ollama and try again.");
        } catch (IOException exception) {
            throw new WeatherApiException(
                    HttpStatus.BAD_GATEWAY, "Could not read a valid response from Ollama.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WeatherApiException(
                    HttpStatus.SERVICE_UNAVAILABLE, "The chat request was interrupted. Please try again.");
        }
    }

    private static boolean validHistoryMessage(ChatMessage message) {
        return message != null
                && ("user".equals(message.role()) || "assistant".equals(message.role()))
                && message.content() != null
                && !message.content().isBlank()
                && message.content().length() <= 4000;
    }
}
