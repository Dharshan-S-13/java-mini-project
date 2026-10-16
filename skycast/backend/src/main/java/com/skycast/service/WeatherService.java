package com.skycast.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycast.model.Forecast;
import com.skycast.model.ForecastItem;
import com.skycast.model.Weather;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class WeatherService {
    private static final String API_BASE_URL =
            "https://api.openweathermap.org/data/2.5/";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String configuredApiKey;

    public WeatherService(
            ObjectMapper objectMapper,
            @Value("${openweather.api-key:}") String configuredApiKey) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.configuredApiKey = configuredApiKey;
    }

    public Weather getWeather(String city) {
        String validatedCity = validateCity(city);
        JsonNode response = fetch("weather", validatedCity);
        JsonNode condition = firstCondition(response);
        JsonNode main = response.path("main");
        JsonNode wind = response.path("wind");
        return new Weather(
                text(response, "name"),
                text(response.path("sys"), "country"),
                number(response, "dt").longValue(),
                number(response, "timezone").intValue(),
                decimal(main, "temp"),
                decimal(main, "feels_like"),
                decimal(main, "humidity"),
                decimal(wind, "speed"),
                decimal(main, "pressure"),
                text(condition, "description"),
                text(condition, "icon"));
    }

    public Forecast getForecast(String city) {
        String validatedCity = validateCity(city);
        JsonNode response = fetch("forecast", validatedCity);
        JsonNode cityNode = response.path("city");
        JsonNode entries = response.path("list");
        if (!entries.isArray()) {
            throw new WeatherApiException(
                    HttpStatus.BAD_GATEWAY, "OpenWeatherMap returned an invalid forecast.");
        }

        List<ForecastItem> items = new ArrayList<>();
        for (JsonNode entry : entries) {
            JsonNode main = entry.path("main");
            JsonNode condition = firstCondition(entry);
            items.add(new ForecastItem(
                    number(entry, "dt").longValue(),
                    text(entry, "dt_txt"),
                    decimal(main, "temp"),
                    decimal(main, "temp_min"),
                    decimal(main, "temp_max"),
                    decimal(main, "humidity"),
                    entry.path("pop").asDouble(0),
                    text(condition, "description"),
                    text(condition, "icon")));
        }
        return new Forecast(
                text(cityNode, "name"),
                text(cityNode, "country"),
                Instant.now().getEpochSecond(),
                number(cityNode, "timezone").intValue(),
                List.copyOf(items));
    }

    public String getWeatherContext(String city) {
        Weather weather = getWeather(city);
        Forecast forecast = getForecast(city);
        StringBuilder context = new StringBuilder()
                .append("Verified OpenWeatherMap data for ")
                .append(weather.city()).append(", ").append(weather.country()).append(". ")
                .append("Current conditions: ").append(weather.temperature())
                .append(" C, ").append(weather.description()).append("; feels like ")
                .append(weather.feelsLike()).append(" C; humidity ")
                .append(weather.humidity()).append("%; wind ")
                .append(weather.windSpeed()).append(" m/s; pressure ")
                .append(weather.pressure()).append(" hPa. Forecast: ");
        forecast.items().stream().limit(40).forEach(item -> context
                .append(item.dateTime()).append(": ")
                .append(item.description()).append(", ")
                .append(item.temperature()).append(" C, rain probability ")
                .append(Math.round(item.precipitation() * 100)).append("%. "));
        return context.toString();
    }

    private JsonNode fetch(String endpoint, String city) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new WeatherApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Weather search is not configured. Set OPENWEATHER_API_KEY or add it to the project-root .env file.");
        }

        String url = API_BASE_URL + endpoint
                + "?q=" + URLEncoder.encode(city, StandardCharsets.UTF_8)
                + "&appid=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&units=metric";
        try {
            String body = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
            return objectMapper.readTree(body);
        } catch (RestClientResponseException exception) {
            throw mapApiError(exception.getStatusCode().value());
        } catch (RestClientException | IOException exception) {
            throw new WeatherApiException(
                    HttpStatus.BAD_GATEWAY, "Could not connect to OpenWeatherMap. Check your connection and try again.");
        }
    }

    private String getApiKey() {
        if (configuredApiKey != null && !configuredApiKey.isBlank()) {
            return configuredApiKey.trim();
        }
        String environmentKey = System.getenv("OPENWEATHER_API_KEY");
        if (environmentKey != null && !environmentKey.isBlank()) {
            return environmentKey.trim();
        }

        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path parentDirectory = workingDirectory.getParent();
        Path repositoryDirectory = parentDirectory == null ? null : parentDirectory.getParent();
        Path[] candidates = {
                workingDirectory.resolve(".env"),
                parentDirectory == null ? null : parentDirectory.resolve(".env"),
                repositoryDirectory == null ? null : repositoryDirectory.resolve(".env")
        };
        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                try {
                    String key = readApiKey(candidate);
                    if (key != null && !key.isBlank()) {
                        return key;
                    }
                } catch (IOException exception) {
                    throw new WeatherApiException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Could not read the project-root .env configuration.");
                }
            }
        }
        return null;
    }

    private static String readApiKey(Path envFile) throws IOException {
        for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
            String entry = line.strip();
            if (entry.startsWith("export ")) {
                entry = entry.substring("export ".length()).stripLeading();
            }
            if (!entry.startsWith("OPENWEATHER_API_KEY=")) {
                continue;
            }
            String value = entry.substring("OPENWEATHER_API_KEY=".length()).strip();
            if (value.length() >= 2
                    && (value.startsWith("\"") && value.endsWith("\"")
                    || value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            int commentStart = value.indexOf(" #");
            return commentStart >= 0 ? value.substring(0, commentStart).stripTrailing() : value;
        }
        return null;
    }

    private static String validateCity(String city) {
        if (city == null || city.isBlank() || city.length() > 100) {
            throw new WeatherApiException(
                    HttpStatus.BAD_REQUEST, "Enter a city name between 1 and 100 characters.");
        }
        return city.trim();
    }

    private static JsonNode firstCondition(JsonNode node) {
        JsonNode conditions = node.path("weather");
        if (!conditions.isArray() || conditions.isEmpty()) {
            throw new WeatherApiException(
                    HttpStatus.BAD_GATEWAY, "OpenWeatherMap returned no weather conditions.");
        }
        return conditions.get(0);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            throw new WeatherApiException(
                    HttpStatus.BAD_GATEWAY, "OpenWeatherMap returned incomplete weather data.");
        }
        return value.asText();
    }

    private static JsonNode number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isNumber()) {
            throw new WeatherApiException(
                    HttpStatus.BAD_GATEWAY, "OpenWeatherMap returned incomplete weather data.");
        }
        return value;
    }

    private static double decimal(JsonNode node, String field) {
        return number(node, field).asDouble();
    }

    private static WeatherApiException mapApiError(int status) {
        return switch (status) {
            case 401 -> new WeatherApiException(
                    HttpStatus.UNAUTHORIZED, "OpenWeatherMap rejected the API key. Check OPENWEATHER_API_KEY.");
            case 404 -> new WeatherApiException(
                    HttpStatus.NOT_FOUND, "City not found. Check the spelling and try again.");
            case 429 -> new WeatherApiException(
                    HttpStatus.TOO_MANY_REQUESTS, "OpenWeatherMap request limit reached. Try again later.");
            case 500, 502, 503, 504 -> new WeatherApiException(
                    HttpStatus.BAD_GATEWAY, "OpenWeatherMap is temporarily unavailable. Try again later.");
            default -> new WeatherApiException(
                    HttpStatus.BAD_GATEWAY, "OpenWeatherMap request failed (HTTP " + status + ").");
        };
    }
}
