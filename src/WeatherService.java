import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherService {

    private final HttpClient client = HttpClient.newHttpClient();

    public String getWeatherData(String city) {

        String apiKey = System.getenv("OPENWEATHER_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("OPENWEATHER_API_KEY is not set.");
            return null;
        }

        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = "https://api.openweathermap.org/data/2.5/weather"
                + "?q=" + encodedCity
                + "&appid=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&units=metric";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                System.out.println("Weather API error (HTTP " + response.statusCode()
                        + "): " + response.body());
                return null;
            }

            return response.body();

        } catch (IOException e) {
            System.out.println("Network error while connecting to the weather API. Check your internet connection and try again.");
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Weather API request was interrupted.");
            return null;
        } catch (IllegalArgumentException e) {
            System.out.println("Could not create a valid weather API request.");
            return null;
        }
    }
}