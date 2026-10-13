import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("===============================");
            System.out.println("          WEATHER APP");
            System.out.println("===============================");
            System.out.print("Press Enter to speak, or type a city: ");
            String input = scanner.nextLine().trim();
            String city = WeatherVoiceInput.normalizeCityRequest(input);
            boolean voiceSearch = input.isEmpty();

            if (voiceSearch) {
                try {
                    city = WeatherVoiceInput.normalizeCityRequest(
                            WindowsSpeechAssistant.listenForCity());
                } catch (IOException e) {
                    System.err.println(
                            "Windows voice input is unavailable: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Voice input was interrupted.");
                    return;
                }

                if (city.isEmpty()) {
                    System.out.print("Type a city name instead: ");
                    city = WeatherVoiceInput.normalizeCityRequest(scanner.nextLine());
                    voiceSearch = false;
                }
            }

            if (city.isEmpty()) {
                System.out.println("City name cannot be empty.");
                return;
            }

            WeatherService service = new WeatherService();
            Weather weather;
            try {
                weather = service.getCurrentWeather(city);
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
                return;
            } catch (IOException e) {
                System.err.println("Could not get weather: " + e.getMessage());
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Weather request was interrupted.");
                return;
            }

            System.out.println();
            System.out.println("Current Weather in " + weather.getCity());
            System.out.printf("Temperature: %.2f\u00B0C%n",
                    weather.getTemperature());
            System.out.printf("Conditions: %s%n", weather.getDescription());
            System.out.printf("Humidity: %.0f%%%n",
                    weather.getHumidity());
            System.out.printf("Wind speed: %.1f m/s%n",
                    weather.getWindSpeed());

            try {
                System.out.println("\n========== WEATHER FORECAST ==========");
                List<Forecast> forecast = service.getForecast(city);
                if (!forecast.isEmpty()) {
                    System.out.printf("%-22s %-12s %-12s%n",
                            "Date & Time", "Temp (C)", "Humidity");
                    System.out.println("-----------------------------------------------");
                    for (Forecast entry : forecast.stream().limit(8).toList()) {
                        System.out.printf("%-22s %-12.2f %-12.0f%n",
                                entry.getDateTime(),
                                entry.getTemperature(),
                                entry.getHumidity());
                    }
                }
            } catch (IOException e) {
                System.err.println(
                        "Could not get the forecast: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Forecast request was interrupted.");
            }

            if (voiceSearch) {
                String summary = String.format(
                        java.util.Locale.ROOT,
                        "In %s, it is %.0f degrees Celsius with %s. "
                                + "Humidity is %.0f percent and wind speed is "
                                + "%.1f meters per second.",
                        weather.getCity(),
                        weather.getTemperature(),
                        weather.getDescription(),
                        weather.getHumidity(),
                        weather.getWindSpeed());
                try {
                    WindowsSpeechAssistant.speak(summary);
                } catch (IOException e) {
                    System.err.println(
                            "Could not speak the weather summary: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Weather speech was interrupted.");
                }
            }
        }
    }
}
