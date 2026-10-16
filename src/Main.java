import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Main {

    private static class ForecastReading {
        LocalDateTime dateTime;
        double temperature;

        ForecastReading(LocalDateTime dateTime, double temperature) {
            this.dateTime = dateTime;
            this.temperature = temperature;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        WeatherService service = new WeatherService();

        System.out.println("===============================");
        System.out.println("          WEATHER APP");
        System.out.println("===============================");
        System.out.print("Enter a city name: ");

        String city = scanner.nextLine().trim();

        String weatherData = service.getWeatherData(city);

        if (weatherData == null) {
            System.out.println("Could not retrieve current weather.");
            scanner.close();
            return;
        }

        try {
            String cityName = weatherData
                    .split("\"name\":\"")[1].split("\"")[0];

            double currentTemp = Double.parseDouble(
                    weatherData.split("\"temp\":")[1].split(",")[0]);

            System.out.println("\nCurrent Weather in " + cityName);
            System.out.printf("Temperature: %.2f\u00B0C%n", currentTemp);
        } catch (Exception e) {
            System.out.println("Could not read current weather data.");
        }

        String forecastData = service.getForecastData(city);

        if (forecastData == null) {
            System.out.println("Could not retrieve forecast data.");
            scanner.close();
            return;
        }

        try {
            ZoneId zone = ZoneId.of("Asia/Kolkata");
            LocalDate today = LocalDate.now(zone);

            List<String> entries = Arrays.stream(
                            forecastData.split("\\{\"dt\":"))
                    .skip(1)
                    .filter(e -> e.contains("\"dt_txt\":\"")
                            && e.contains("\"temp\":"))
                    .collect(Collectors.toList());

            Map<LocalDate, List<ForecastReading>> dailyForecasts =
                    entries.stream()
                            .map(e -> {
                                String dateTime = e
                                        .split("\"dt_txt\":\"")[1]
                                        .split("\"")[0];

                                LocalDateTime localDateTime =
                                        LocalDateTime.parse(
                                                dateTime.replace(" ", "T"))
                                                .atZone(ZoneOffset.UTC)
                                                .withZoneSameInstant(zone)
                                                .toLocalDateTime();

                                double temp = Double.parseDouble(
                                        e.split("\"temp\":")[1].split(",")[0]);

                                return new ForecastReading(localDateTime, temp);
                            })
                            .collect(Collectors.groupingBy(
                                    r -> r.dateTime.toLocalDate(),
                                    TreeMap::new,
                                    Collectors.toList()));

            System.out.println("\n========== TODAY'S TEMPERATURES ==========");

            List<ForecastReading> todayReadings = dailyForecasts.get(today);

            if (todayReadings == null || todayReadings.isEmpty()) {
                System.out.println("No forecast readings available for today.");
            } else {
                todayReadings.stream()
                        .sorted((a, b) -> a.dateTime.compareTo(b.dateTime))
                        .forEach(r -> System.out.printf("%s  %.2f\u00B0C%n",
                                r.dateTime.toLocalTime().toString().substring(0, 5),
                                r.temperature));
            }

            System.out.println(
                    "\n========== NEXT 5 DAYS: AVERAGE TEMPERATURE ==========");

            for (int day = 1; day <= 5; day++) {
                LocalDate date = today.plusDays(day);
                List<ForecastReading> readings = dailyForecasts.get(date);

                if (readings == null || readings.isEmpty()) {
                    System.out.println(date + " : Forecast unavailable");
                } else {
                    double average = readings.stream()
                            .mapToDouble(r -> r.temperature)
                            .average()
                            .orElse(0.0);

                    System.out.printf("%s : Average %.2f\u00B0C%n",
                            date, average);
                }
            }
        } catch (Exception e) {
            System.out.println("Could not process forecast data.");
            e.printStackTrace();
        }

        scanner.close();
    }
}
