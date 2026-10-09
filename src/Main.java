import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("          WEATHER APP");
        System.out.println("=================================");

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter a city name: ");
            String city = scanner.nextLine().trim();

            if (city.isEmpty()) {
                System.out.println("City name cannot be empty.");
                return;
            }

            WeatherService weatherService = new WeatherService();
            String weatherData = weatherService.getWeatherData(city);

            if (weatherData != null) {
                System.out.println(weatherData);
            } else {
                System.out.println("Unable to retrieve weather for " + city + ".");
                System.out.println("Check the message above, then verify your API key and internet connection.");
            }
        }
    }
}
