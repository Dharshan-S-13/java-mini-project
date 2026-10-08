public class Main {

    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("          CHENNAI WEATHER");
        System.out.println("=================================");

        WeatherService weatherService = new WeatherService();
        String weatherData = weatherService.getWeatherData("Chennai");

        if (weatherData != null) {
            System.out.println(weatherData);
        } else {
            System.out.println("Unable to retrieve Chennai weather.");
            System.out.println("Check your API key, internet connection, and API request, then try again.");
        }
    }
}
