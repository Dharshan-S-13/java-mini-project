import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("===============================");
            System.out.println("          WEATHER APP");
            System.out.println("===============================");

            System.out.print("Enter a city name: ");
            String city = scanner.nextLine().trim();

            if (city.isEmpty()) {
                System.out.println("City name cannot be empty.");
                return;
            }

            WeatherService service = new WeatherService();
            String data = service.getWeatherData(city);

            if (data == null) {
                System.out.println("Unable to retrieve current weather.");
                return;
            }

            try {
                String actualCity =
                    data.split("\"name\":\"")[1].split("\"")[0];

                double temperature = Double.parseDouble(
                    data.split("\"temp\":")[1].split(",")[0]);

                System.out.println("\nCurrent Weather in " + actualCity);
                System.out.printf("Temperature: %.2f\u00B0C%n", temperature);
            } catch (Exception e) {
                System.out.println("Could not read current weather.");
                return;
            }

            System.out.println("\n========== WEATHER FORECAST ==========");

            String forecastData = service.getForecastData(city);

            if (forecastData == null) {
                System.out.println("Could not retrieve forecast data.");
                return;
            }

            try {
                String[] entries = forecastData.split("\\{\"dt\":");
                int count = 0;

                System.out.printf("%-22s %-12s %-12s%n",
                    "Date & Time", "Temp (C)", "Humidity");
                System.out.println("-----------------------------------------------");

                for (int i = 1; i < entries.length && count < 8; i++) {
                    String entry = entries[i];

                    String dateTime =
                        entry.split("\"dt_txt\":\"")[1].split("\"")[0];

                    double temp = Double.parseDouble(
                        entry.split("\"temp\":")[1].split(",")[0]);

                    int humidity = Integer.parseInt(
                        entry.split("\"humidity\":")[1].split(",")[0]);

                    System.out.printf("%-22s %-12.2f %-12d%n",
                        dateTime, temp, humidity);
                    count++;
                }

                if (count == 0) {
                    System.out.println("No forecast entries found.");
                }
            } catch (Exception e) {
                System.out.println("Could not parse forecast data.");
            }
        }
    }
}
