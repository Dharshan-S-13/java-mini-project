import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OllamaService {
    private static final String API_URL = "http://127.0.0.1:11434/api/chat";
    private static final String DEFAULT_MODEL = "qwen3.5:latest";
    private static final String SYSTEM_PROMPT =
            "You are SkyCast's helpful website assistant. Be clear, friendly, and concise. "
                    + "When the conversation includes CURRENT SKYCAST WEATHER DATA, use those "
                    + "values to answer current-weather and forecast questions, and do not say "
                    + "you lack live weather access. The supplied data is authoritative for "
                    + "this conversation. Never invent weather values that are not supplied.";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String chat(String conversation) throws IOException, InterruptedException {
        String model = System.getenv("OLLAMA_MODEL");
        if (model == null || model.isBlank()) {
            model = DEFAULT_MODEL;
        }

        String requestBody = "{\"model\":\"" + escapeJson(model.trim())
                + "\",\"stream\":false,\"think\":false,"
                + "\"options\":{\"num_predict\":512},\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(SYSTEM_PROMPT) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(conversation) + "\"}]}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            String detail = response.body().isBlank()
                    ? "HTTP " + response.statusCode()
                    : response.body();
            throw new IOException("Ollama could not answer: " + detail);
        }
        return response.body();
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
