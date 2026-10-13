import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WeatherService {

    private static final String API_BASE_URL =
            "https://api.openweathermap.org/data/2.5/";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String getWeatherData(String city) {
        return fetchWeatherData(city, "weather");
    }

    public String getForecastData(String city) {
        return fetchWeatherData(city, "forecast");
    }

    public Weather getCurrentWeather(String city)
            throws IOException, InterruptedException {
        validateCity(city);
        String response = requestWeatherData(city, "weather");
        return parseWeather(response);
    }

    public List<Forecast> getForecast(String city)
            throws IOException, InterruptedException {
        validateCity(city);
        String response = requestWeatherData(city, "forecast");
        return parseForecast(response);
    }

    private String fetchWeatherData(String city, String endpoint) {
        try {
            validateCity(city);
            return requestWeatherData(city, endpoint);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Weather request was interrupted.");
            return null;
        }
    }

    private String requestWeatherData(String city, String endpoint)
            throws IOException, InterruptedException {
        String apiKey = getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException(
                    "OPENWEATHER_API_KEY was not found. Set it in the environment or add it to the project-root .env file.");
        }

        String encodedCity = URLEncoder.encode(
                city, StandardCharsets.UTF_8);

        String url = API_BASE_URL
                + endpoint
                + "?q=" + encodedCity
                + "&appid=" + URLEncoder.encode(
                        apiKey, StandardCharsets.UTF_8)
                + "&units=metric";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(getApiErrorMessage(response.statusCode()));
        }
        return response.body();
    }

    public static String getApiKey() throws IOException {
        String apiKey = System.getenv("OPENWEATHER_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }

        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path parentDirectory = workingDirectory.getParent();
        Path[] envFiles = {
                workingDirectory.resolve(".env"),
                workingDirectory.resolve(".env.txt"),
                parentDirectory != null ? parentDirectory.resolve(".env") : null,
                parentDirectory != null ? parentDirectory.resolve(".env.txt") : null
        };

        for (Path envFile : envFiles) {
            if (envFile != null && Files.isRegularFile(envFile)) {
                String apiKeyFromFile = readApiKey(envFile);
                if (apiKeyFromFile != null && !apiKeyFromFile.isBlank()) {
                    return apiKeyFromFile;
                }
            }
        }
        return null;
    }

    private static String readApiKey(Path envFile) throws IOException {
        for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
            String entry = line.strip();
            if (entry.startsWith("\uFEFF")) {
                entry = entry.substring(1).stripLeading();
            }
            if (entry.startsWith("export ")) {
                entry = entry.substring("export ".length()).stripLeading();
            }
            if (!entry.startsWith("OPENWEATHER_API_KEY")) {
                continue;
            }

            int separator = entry.indexOf('=');
            if (separator < 0
                    || !entry.substring(0, separator).strip()
                            .equals("OPENWEATHER_API_KEY")) {
                continue;
            }

            String value = entry.substring(separator + 1).strip();
            if (!value.isEmpty()
                    && (value.charAt(0) == '"' || value.charAt(0) == '\'')) {
                char quote = value.charAt(0);
                int closingQuote = value.indexOf(quote, 1);
                if (closingQuote < 0
                        || !hasOnlyCommentAfter(value, closingQuote + 1)) {
                    throw new IOException(
                            "The project-root .env file has an invalid OPENWEATHER_API_KEY entry.");
                }
                value = value.substring(1, closingQuote);
            } else {
                int comment = value.indexOf(" #");
                if (comment >= 0) {
                    value = value.substring(0, comment).stripTrailing();
                }
            }
            if (!value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static boolean hasOnlyCommentAfter(String value, int position) {
        String trailing = value.substring(position).strip();
        return trailing.isEmpty() || trailing.startsWith("#");
    }

    private static Weather parseWeather(String response) throws IOException {
        Map<String, Object> root = asObject(new JsonReader(response).read(), "weather response");
        Map<String, Object> main = getObject(root, "main");
        Map<String, Object> wind = getObject(root, "wind");
        List<?> conditions = getArray(root, "weather");

        if (conditions.isEmpty()) {
            throw new IOException(
                    "OpenWeatherMap returned no weather conditions.");
        }

        Map<String, Object> condition = asObject(conditions.get(0), "weather condition");
        return new Weather(
                getString(root, "name"),
                getNumber(main, "temp"),
                getNumber(main, "humidity"),
                getNumber(wind, "speed"),
                getString(condition, "description"));
    }

    private static List<Forecast> parseForecast(String response)
            throws IOException {
        Map<String, Object> root = asObject(new JsonReader(response).read(), "forecast response");
        List<?> entries = getArray(root, "list");
        List<Forecast> forecast = new ArrayList<>(entries.size());

        for (Object entry : entries) {
            Map<String, Object> item = asObject(entry, "forecast entry");
            Map<String, Object> main = getObject(item, "main");
            forecast.add(new Forecast(
                    getString(item, "dt_txt"),
                    getNumber(main, "temp"),
                    getNumber(main, "humidity")));
        }
        return List.copyOf(forecast);
    }

    private static Map<String, Object> getObject(
            Map<String, Object> parent, String name)
            throws IOException {
        Object value = parent.get(name);
        if (!(value instanceof Map<?, ?>)) {
            throw new IOException(
                    "OpenWeatherMap response is missing " + name + " data.");
        }
        return asObject(value, name);
    }

    private static List<?> getArray(Map<String, Object> parent, String name)
            throws IOException {
        Object value = parent.get(name);
        if (!(value instanceof List<?> array)) {
            throw new IOException(
                    "OpenWeatherMap response is missing " + name + " data.");
        }
        return array;
    }

    private static Map<String, Object> asObject(Object value, String name)
            throws IOException {
        if (!(value instanceof Map<?, ?> object)) {
            throw new IOException(
                    "OpenWeatherMap returned invalid " + name + " data.");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) object;
        return result;
    }

    private static String getString(Map<String, Object> parent, String name)
            throws IOException {
        Object value = parent.get(name);
        if (!(value instanceof String text)) {
            throw new IOException(
                    "OpenWeatherMap response is missing " + name + " data.");
        }
        return text;
    }

    private static double getNumber(
            Map<String, Object> parent, String name) throws IOException {
        Object value = parent.get(name);
        if (!(value instanceof Number number)) {
            throw new IOException(
                    "OpenWeatherMap response is missing " + name + " data.");
        }

        double result = number.doubleValue();
        if (!Double.isFinite(result)) {
            throw new IOException(
                    "OpenWeatherMap returned an invalid " + name + " value.");
        }
        return result;
    }

    private static final class JsonReader {
        private final String input;
        private int position;

        private JsonReader(String input) {
            this.input = input;
        }

        private Object read() throws IOException {
            skipWhitespace();
            Object value = readValue();
            skipWhitespace();
            if (position != input.length()) {
                throw error("Unexpected content after JSON value");
            }
            return value;
        }

        private Object readValue() throws IOException {
            if (position >= input.length()) {
                throw error("Unexpected end of JSON");
            }

            return switch (input.charAt(position)) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> {
                    if (input.charAt(position) == '-'
                            || isDigit(input.charAt(position))) {
                        yield readNumber();
                    }
                    throw error("Unexpected character");
                }
            };
        }

        private Map<String, Object> readObject() throws IOException {
            position++;
            skipWhitespace();
            Map<String, Object> object = new LinkedHashMap<>();
            if (consume('}')) {
                return object;
            }

            while (true) {
                skipWhitespace();
                if (position >= input.length() || input.charAt(position) != '"') {
                    throw error("Expected an object key");
                }
                String key = readString();
                skipWhitespace();
                require(':');
                skipWhitespace();
                if (object.containsKey(key)) {
                    throw error("Duplicate object key");
                }
                object.put(key, readValue());
                skipWhitespace();
                if (consume('}')) {
                    return object;
                }
                require(',');
            }
        }

        private List<Object> readArray() throws IOException {
            position++;
            skipWhitespace();
            List<Object> array = new ArrayList<>();
            if (consume(']')) {
                return array;
            }

            while (true) {
                skipWhitespace();
                array.add(readValue());
                skipWhitespace();
                if (consume(']')) {
                    return array;
                }
                require(',');
            }
        }

        private String readString() throws IOException {
            require('"');
            StringBuilder value = new StringBuilder();
            while (position < input.length()) {
                char current = input.charAt(position++);
                if (current == '"') {
                    return value.toString();
                }
                if (current < 0x20) {
                    throw error("Unescaped control character in string");
                }
                if (current != '\\') {
                    value.append(current);
                    continue;
                }
                if (position >= input.length()) {
                    throw error("Incomplete escape sequence");
                }

                char escaped = input.charAt(position++);
                switch (escaped) {
                    case '"', '\\', '/' -> value.append(escaped);
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(readUnicodeEscape());
                    default -> throw error("Invalid escape sequence");
                }
            }
            throw error("Unterminated string");
        }

        private char readUnicodeEscape() throws IOException {
            if (input.length() - position < 4) {
                throw error("Incomplete Unicode escape");
            }

            int codeUnit = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(input.charAt(position++), 16);
                if (digit < 0) {
                    throw error("Invalid Unicode escape");
                }
                codeUnit = codeUnit * 16 + digit;
            }
            return (char) codeUnit;
        }

        private BigDecimal readNumber() throws IOException {
            int start = position;
            consume('-');

            if (consume('0')) {
                if (position < input.length() && isDigit(input.charAt(position))) {
                    throw error("Leading zero in number");
                }
            } else {
                requireDigitOneToNine();
                while (position < input.length() && isDigit(input.charAt(position))) {
                    position++;
                }
            }

            if (consume('.')) {
                requireDigit();
                while (position < input.length() && isDigit(input.charAt(position))) {
                    position++;
                }
            }

            if (consume('e') || consume('E')) {
                if (!consume('+')) {
                    consume('-');
                }
                requireDigit();
                while (position < input.length() && isDigit(input.charAt(position))) {
                    position++;
                }
            }

            try {
                return new BigDecimal(input.substring(start, position));
            } catch (NumberFormatException e) {
                throw error("Invalid number");
            }
        }

        private Object readLiteral(String literal, Object value)
                throws IOException {
            if (!input.startsWith(literal, position)) {
                throw error("Invalid JSON literal");
            }
            position += literal.length();
            return value;
        }

        private void requireDigit() throws IOException {
            if (position >= input.length() || !isDigit(input.charAt(position))) {
                throw error("Expected a digit");
            }
            position++;
        }

        private void requireDigitOneToNine() throws IOException {
            if (position >= input.length()
                    || input.charAt(position) < '1'
                    || input.charAt(position) > '9') {
                throw error("Expected a digit");
            }
            position++;
        }

        private void require(char expected) throws IOException {
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'");
            }
        }

        private boolean consume(char expected) {
            if (position < input.length()
                    && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < input.length()) {
                char current = input.charAt(position);
                if (current != ' ' && current != '\t'
                        && current != '\n' && current != '\r') {
                    return;
                }
                position++;
            }
        }

        private IOException error(String message) {
            return new IOException(
                    "Invalid JSON at character " + position + ": " + message + ".");
        }

        private static boolean isDigit(char value) {
            return value >= '0' && value <= '9';
        }
    }

    private static void validateCity(String city) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City name cannot be empty.");
        }
        if (city.length() > 100) {
            throw new IllegalArgumentException(
                    "City name cannot be longer than 100 characters.");
        }
    }

    private static String getApiErrorMessage(int statusCode) {
        return switch (statusCode) {
            case 401 -> "OpenWeatherMap rejected the API key. Check your OPENWEATHER_API_KEY.";
            case 404 -> "City not found. Check the spelling and try again.";
            case 429 -> "OpenWeatherMap request limit reached. Try again later.";
            case 500, 502, 503, 504 ->
                    "OpenWeatherMap is temporarily unavailable. Try again later.";
            default -> "OpenWeatherMap request failed (HTTP "
                    + statusCode + ").";
        };
    }
}
