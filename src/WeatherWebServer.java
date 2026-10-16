import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherWebServer {

    private static final int MAX_CHAT_REQUEST_BYTES = 16_384;
    private static final int MAX_CHAT_MESSAGE_LENGTH = 2_000;
    private static final int MAX_CITY_LENGTH = 100;
    private static final Path WEB_DIRECTORY = Path.of("web");
    private static final Pattern FORECAST_ENTRY = Pattern.compile(
            "\\{\\s*\"dt\"\\s*:\\s*\\d+.*?\"temp\"\\s*:\\s*(-?(?:\\d+(?:\\.\\d*)?|\\.\\d+))"
                    + ".*?\"dt_txt\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.DOTALL);

    public static void main(String[] args) throws IOException {
        int port = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        WeatherService service = new WeatherService();
        GeminiChat chat = new GeminiChat();

        server.createContext("/api/weather", exchange -> {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.getResponseHeaders().set("Allow", "GET");
                sendJson(exchange, 405,
                        "{\"error\":\"Only GET requests are supported.\"}");
                return;
            }

            String city;
            try {
                city = getCityParameter(exchange.getRequestURI().getRawQuery());
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400,
                        "{\"error\":\"The city name is not valid URL text.\"}");
                return;
            }

            if (city == null || city.isBlank()) {
                sendJson(exchange, 400,
                        "{\"error\":\"Enter a city name to search.\"}");
                return;
            }

            String current = service.getWeatherData(city);
            String forecast = service.getForecastData(city);
            if (current == null || forecast == null) {
                sendJson(exchange, 502,
                        "{\"error\":\"Weather data is unavailable. Check the city spelling, API key, and internet connection.\"}");
                return;
            }

            try {
                sendJson(exchange, 200, buildWeatherResponse(current, forecast));
            } catch (IllegalArgumentException e) {
                System.err.println("Could not parse weather API response: "
                        + e.getMessage());
                sendJson(exchange, 502,
                        "{\"error\":\"Weather data could not be processed. Please try again.\"}");
            }
        });

        server.createContext("/api/chat", exchange ->
                handleChat(exchange, service, chat));

        server.createContext("/style.css", exchange ->
                serveAsset(exchange, "style.css", "text/css; charset=UTF-8"));
        server.createContext("/app.js", exchange ->
                serveAsset(exchange, "app.js",
                        "text/javascript; charset=UTF-8"));

        server.createContext("/", exchange -> {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.getResponseHeaders().set("Allow", "GET");
                send(exchange, 405, "text/plain; charset=UTF-8",
                        "Only GET requests are supported.");
                return;
            }
            if (!exchange.getRequestURI().getPath().equals("/")) {
                send(exchange, 404, "text/plain; charset=UTF-8",
                        "Page not found.");
                return;
            }

            Path indexPath = WEB_DIRECTORY.resolve("index.html");
            if (!Files.isRegularFile(indexPath)) {
                send(exchange, 500, "text/plain; charset=UTF-8",
                        "Website file not found. Run the server from the project folder and make sure web/index.html exists.");
                return;
            }
            send(exchange, 200, "text/html; charset=UTF-8",
                    Files.readString(indexPath, StandardCharsets.UTF_8));
        });

        server.start();
        System.out.println("Weather website running at http://localhost:" + port);
        System.out.println("Keep this terminal open.");
    }

    private static void handleChat(HttpExchange exchange, WeatherService service,
                                   GeminiChat chat) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            exchange.getResponseHeaders().set("Allow", "POST");
            sendJson(exchange, 405,
                    "{\"error\":\"Only POST requests are supported.\"}");
            return;
        }

        byte[] requestBytes = exchange.getRequestBody()
                .readNBytes(MAX_CHAT_REQUEST_BYTES + 1);
        if (requestBytes.length > MAX_CHAT_REQUEST_BYTES) {
            sendJson(exchange, 413,
                    "{\"error\":\"Your message is too long. Please shorten it and try again.\"}");
            return;
        }

        String message;
        String city;
        try {
            String body = new String(requestBytes, StandardCharsets.UTF_8);
            message = getJsonStringField(body, "message");
            city = getJsonStringField(body, "city");
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400,
                    "{\"error\":\"The chat request was not valid JSON.\"}");
            return;
        }

        if (message == null || message.isBlank()) {
            sendJson(exchange, 400,
                    "{\"error\":\"Enter a message before sending.\"}");
            return;
        }
        if (message.length() > MAX_CHAT_MESSAGE_LENGTH) {
            sendJson(exchange, 413,
                    "{\"error\":\"Your message is too long. Please shorten it and try again.\"}");
            return;
        }
        if (city == null || city.isBlank() || city.length() > MAX_CITY_LENGTH) {
            sendJson(exchange, 400,
                    "{\"error\":\"Enter a valid city in the weather search box.\"}");
            return;
        }

        String current = service.getWeatherData(city);
        String forecast = service.getForecastData(city);
        String context = current == null || forecast == null
                ? "Weather data is currently unavailable for " + city + "."
                : buildWeatherContext(city, current, forecast);

        try {
            String reply = chat.ask(message.trim(), context);
            sendJson(exchange, 200,
                    "{\"reply\":\"" + escapeJson(reply) + "\"}");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendJson(exchange, 503,
                    "{\"error\":\"The weather assistant was interrupted. Please try again.\"}");
        } catch (IOException e) {
            System.err.println("Gemini chat request failed: " + e.getMessage());
            String errorMessage = "The AI assistant is temporarily unavailable. Please try again.";
            if ("GEMINI_API_KEY is not set.".equals(e.getMessage())) {
                errorMessage = "The AI assistant is not configured yet. Set GEMINI_API_KEY and restart the server.";
            }
            sendJson(exchange, 502,
                    "{\"error\":\"" + escapeJson(errorMessage) + "\"}");
        }
    }

    private static void serveAsset(HttpExchange exchange, String filename,
                                   String contentType) throws IOException {
        String expectedPath = "/" + filename;
        if (!exchange.getRequestURI().getPath().equals(expectedPath)) {
            send(exchange, 404, "text/plain; charset=UTF-8", "File not found.");
            return;
        }
        if (!exchange.getRequestMethod().equals("GET")) {
            exchange.getResponseHeaders().set("Allow", "GET");
            send(exchange, 405, "text/plain; charset=UTF-8",
                    "Only GET requests are supported.");
            return;
        }

        Path assetPath = WEB_DIRECTORY.resolve(filename);
        if (!Files.isRegularFile(assetPath)) {
            send(exchange, 404, "text/plain; charset=UTF-8", "File not found.");
            return;
        }
        send(exchange, 200, contentType,
                Files.readString(assetPath, StandardCharsets.UTF_8));
    }

    private static String buildWeatherResponse(String current, String forecast) {
        String city = getJsonStringField(current, "name");
        String description = getJsonStringField(current, "description");
        Double temperature = getJsonNumberField(current, "temp");
        Double feelsLike = getJsonNumberField(current, "feels_like");
        Double humidity = getJsonNumberField(current, "humidity");
        Double windSpeed = getJsonNumberField(current, "speed");

        if (city == null || description == null || temperature == null
                || feelsLike == null || humidity == null || windSpeed == null) {
            throw new IllegalArgumentException("Missing current weather fields.");
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        Map<LocalDate, List<Double>> dailyTemperatures = new TreeMap<>();
        List<String> todayReadings = new ArrayList<>();
        Matcher matcher = FORECAST_ENTRY.matcher(forecast);

        while (matcher.find()) {
            double readingTemperature = Double.parseDouble(matcher.group(1));
            LocalDateTime dateTime = parseForecastDateTime(matcher.group(2));
            dailyTemperatures.computeIfAbsent(dateTime.toLocalDate(),
                    date -> new ArrayList<>()).add(readingTemperature);
            if (dateTime.toLocalDate().equals(today)) {
                todayReadings.add("{\"time\":\""
                        + escapeJson(dateTime.toLocalTime().toString().substring(0, 5))
                        + "\",\"temperature\":" + readingTemperature + "}");
            }
        }

        StringBuilder response = new StringBuilder();
        response.append("{\"city\":\"").append(escapeJson(city))
                .append("\",\"current\":{\"description\":\"")
                .append(escapeJson(description))
                .append("\",\"temperature\":").append(temperature)
                .append(",\"feelsLike\":").append(feelsLike)
                .append(",\"humidity\":").append(humidity)
                .append(",\"windSpeed\":").append(windSpeed)
                .append("},\"today\":[")
                .append(String.join(",", todayReadings))
                .append("],\"daily\":[");

        for (int day = 1; day <= 5; day++) {
            if (day > 1) {
                response.append(',');
            }
            LocalDate date = today.plusDays(day);
            List<Double> readings = dailyTemperatures.get(date);
            response.append("{\"date\":\"").append(date).append("\",\"average\":");
            if (readings == null || readings.isEmpty()) {
                response.append("null");
            } else {
                response.append(readings.stream().mapToDouble(Double::doubleValue)
                        .average().orElseThrow());
            }
            response.append('}');
        }
        return response.append("]}").toString();
    }

    private static String buildWeatherContext(String city, String current,
                                              String forecast) {
        String description = getJsonStringField(current, "description");
        Double temperature = getJsonNumberField(current, "temp");
        Double feelsLike = getJsonNumberField(current, "feels_like");
        Double humidity = getJsonNumberField(current, "humidity");
        Double windSpeed = getJsonNumberField(current, "speed");
        StringBuilder context = new StringBuilder()
                .append("City: ").append(city).append('\n')
                .append("Current conditions: ")
                .append(description == null ? "unavailable" : description)
                .append('\n')
                .append("Temperature: ").append(valueOrUnavailable(temperature))
                .append(" °C\nFeels like: ")
                .append(valueOrUnavailable(feelsLike))
                .append(" °C\nHumidity: ")
                .append(valueOrUnavailable(humidity))
                .append("%\nWind speed: ")
                .append(valueOrUnavailable(windSpeed))
                .append(" m/s\n");

        Matcher matcher = FORECAST_ENTRY.matcher(forecast);
        context.append("Forecast readings:\n");
        int count = 0;
        while (matcher.find() && count < 40) {
            context.append(parseForecastDateTime(matcher.group(2))).append(": ")
                    .append(matcher.group(1)).append(" °C\n");
            count++;
        }
        if (count == 0) {
            context.append("No forecast readings available.\n");
        }
        return context.toString();
    }

    private static LocalDateTime parseForecastDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime.replace(" ", "T"))
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
                .toLocalDateTime();
    }

    private static String valueOrUnavailable(Double value) {
        return value == null ? "unavailable" : value.toString();
    }

    private static String getCityParameter(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        for (String part : rawQuery.split("&")) {
            String[] pair = part.split("=", 2);
            String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            if (name.equals("city")) {
                return pair.length == 2
                        ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8).trim()
                        : "";
            }
        }
        return null;
    }

    private static String getJsonStringField(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key)
                + "\"\\s*:\\s*(\"(?:\\\\.|[^\"\\\\])*\")");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? decodeJsonString(matcher.group(1)) : null;
    }

    private static Double getJsonNumberField(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key)
                + "\"\\s*:\\s*(-?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Double.valueOf(matcher.group(1)) : null;
    }

    private static String decodeJsonString(String jsonString) {
        StringBuilder decoded = new StringBuilder();
        for (int i = 1; i < jsonString.length() - 1; i++) {
            char character = jsonString.charAt(i);
            if (character != '\\') {
                decoded.append(character);
                continue;
            }
            if (++i >= jsonString.length() - 1) {
                throw new IllegalArgumentException("Invalid JSON escape.");
            }
            char escaped = jsonString.charAt(i);
            switch (escaped) {
                case '"', '\\', '/' -> decoded.append(escaped);
                case 'b' -> decoded.append('\b');
                case 'f' -> decoded.append('\f');
                case 'n' -> decoded.append('\n');
                case 'r' -> decoded.append('\r');
                case 't' -> decoded.append('\t');
                case 'u' -> {
                    if (i + 4 >= jsonString.length() - 1) {
                        throw new IllegalArgumentException("Invalid JSON unicode escape.");
                    }
                    try {
                        decoded.append((char) Integer.parseInt(
                                jsonString.substring(i + 1, i + 5), 16));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid JSON unicode escape.", e);
                    }
                    i += 4;
                }
                default -> throw new IllegalArgumentException("Invalid JSON escape.");
            }
        }
        return decoded.toString();
    }

    private static String escapeJson(String text) {
        StringBuilder escaped = new StringBuilder();
        for (char character : text.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                default -> {
                    if (character < 32) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static void sendJson(HttpExchange exchange, int status, String body)
            throws IOException {
        send(exchange, status, "application/json; charset=UTF-8", body);
    }

    private static void send(HttpExchange exchange, int status,
                             String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
