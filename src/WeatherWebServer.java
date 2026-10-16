import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class WeatherWebServer {
    private static final int PORT = 8080;
    private static final int MAX_CHAT_REQUEST_BYTES = 64 * 1024;
    private final WeatherService weatherService = new WeatherService();
    private final OllamaService ollamaService = new OllamaService();

    public static void main(String[] args) throws IOException {
        new WeatherWebServer().start();
    }

    private void start() throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/", this::handleRequest);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("VaanilAI server started on http://localhost:" + PORT);
        System.out.println("Keep this terminal open while using the app.");
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        switch (path) {
            case "/api/weather" -> handleWeather(exchange, false);
            case "/api/forecast" -> handleWeather(exchange, true);
            case "/api/chat" -> handleChat(exchange);
            case "/" -> serveAsset(exchange, "index.html", "text/html; charset=utf-8");
            case "/styles.css" -> serveAsset(exchange, "styles.css", "text/css; charset=utf-8");
            case "/app.js" -> serveAsset(exchange, "app.js", "text/javascript; charset=utf-8");
            default -> sendError(exchange, 404, "Page not found.");
        }
    }

    private void handleWeather(HttpExchange exchange, boolean forecast)
            throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            sendError(exchange, 405, "Only GET requests are supported.");
            return;
        }

        String city;
        try {
            city = getQueryParameter(exchange, "city").trim();
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, "The city query is not valid.");
            return;
        }

        if (city.isEmpty() || city.length() > 100) {
            sendError(exchange, 400, "Enter a city name between 1 and 100 characters.");
            return;
        }

        String apiKey;
        try {
            apiKey = WeatherService.getApiKey();
        } catch (IOException e) {
            sendError(exchange, 500, "Error reading API configuration: " + e.getMessage());
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            sendError(exchange, 503,
                    "Weather search is not configured. Set OPENWEATHER_API_KEY or add it to .env and restart the server.");
            return;
        }

        String response = forecast
                ? weatherService.getForecastData(city)
                : weatherService.getWeatherData(city);
        if (response == null) {
            sendError(exchange, 502,
                    "Weather data could not be loaded. Check the city, API key, and internet connection.");
            return;
        }

        send(exchange, 200, "application/json; charset=utf-8", response);
    }

    private void handleChat(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "POST");
            sendError(exchange, 405, "Only POST requests are supported.");
            return;
        }

        byte[] requestBytes = exchange.getRequestBody()
                .readNBytes(MAX_CHAT_REQUEST_BYTES + 1);
        if (requestBytes.length > MAX_CHAT_REQUEST_BYTES) {
            sendError(exchange, 413, "Chat message is too long.");
            return;
        }

        String conversation = new String(requestBytes, StandardCharsets.UTF_8).trim();
        if (conversation.isEmpty()) {
            sendError(exchange, 400, "Enter a message to chat.");
            return;
        }

        try {
            String response = ollamaService.chat(conversation);
            send(exchange, 200, "application/json; charset=utf-8", response);
        } catch (HttpTimeoutException e) {
            sendError(exchange, 504,
                    "Qwen took too long to respond. Try a shorter message or check Ollama's CPU usage.");
        } catch (IOException e) {
            sendError(exchange, 502,
                    "Could not get a response from Ollama at http://127.0.0.1:11434. "
                            + "Make sure Ollama is running and the configured model is installed. "
                            + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendError(exchange, 503, "The chat request was interrupted. Please try again.");
        }
    }

    private static String getQueryParameter(HttpExchange exchange, String name) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) {
            return "";
        }

        for (String parameter : query.split("&")) {
            int separator = parameter.indexOf('=');
            String key = separator < 0 ? parameter : parameter.substring(0, separator);
            if (name.equals(URLDecoder.decode(key, StandardCharsets.UTF_8))) {
                return separator < 0 ? ""
                        : URLDecoder.decode(
                                parameter.substring(separator + 1),
                                StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static void serveAsset(
            HttpExchange exchange, String name, String contentType)
            throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            sendError(exchange, 405, "Only GET requests are supported.");
            return;
        }

        byte[] bytes;
        try (InputStream resource =
                     WeatherWebServer.class.getResourceAsStream("/" + name)) {
            if (resource != null) {
                bytes = resource.readAllBytes();
            } else {
                java.nio.file.Path filePath = java.nio.file.Path.of(name);
                if (!java.nio.file.Files.isRegularFile(filePath)) {
                    filePath = java.nio.file.Path.of("src", name);
                }
                if (!java.nio.file.Files.isRegularFile(filePath)) {
                    sendError(exchange, 404, "Page not found.");
                    return;
                }
                bytes = java.nio.file.Files.readAllBytes(filePath);
            }
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            send(exchange, 200, contentType, bytes);
        }
    }

    private static void sendError(
            HttpExchange exchange, int status, String message) throws IOException {
        send(exchange, status, "application/json; charset=utf-8",
                "{\"error\":\"" + escapeJson(message) + "\"}");
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

    private static void send(
            HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        send(exchange, status, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(
            HttpExchange exchange, int status, String contentType, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
