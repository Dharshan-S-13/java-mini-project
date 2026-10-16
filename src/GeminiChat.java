
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiChat {

    private static final String MODEL = "gemini-3.8-flash";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public String ask(String message, String weatherContext)
            throws IOException, InterruptedException {

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("GEMINI_API_KEY is not set.");
        }

        String prompt = """
                You are a friendly weather assistant in a Java weather app.
                Answer clearly and briefly.
                Use the supplied weather data for current conditions and forecasts.
                Never invent weather information.
                If weather data is unavailable, tell the user.

                Weather data:
                %s

                User question:
                %s
                """.formatted(weatherContext, message);

        String requestBody = """
                {"contents":[{"parts":[{"text":"%s"}]}]}
                """.formatted(escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(
                    requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            System.err.println("Gemini HTTP status: " + response.statusCode());
            System.err.println("Gemini response: " + response.body());
            throw new IOException(
                    "Gemini request failed. Check the server terminal for details.");
        }

        Matcher matcher = Pattern.compile(
                "\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                .matcher(response.body());

        if (!matcher.find()) {
            throw new IOException("Gemini returned no text response.");
        }

        return unescapeJson(matcher.group(1));
    }

    private static String escapeJson(String text) {
        StringBuilder out = new StringBuilder();

        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }

        return out.toString();
    }

    private static String unescapeJson(String text) {
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c != '\\' || i + 1 >= text.length()) {
                out.append(c);
                continue;
            }

            char next = text.charAt(++i);

            switch (next) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    if (i + 4 < text.length()) {
                        out.append((char) Integer.parseInt(
                                text.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                }
                default -> out.append(next);
            }
        }

        return out.toString();
    }
}

