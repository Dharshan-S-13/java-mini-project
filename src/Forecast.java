public class Forecast {
    private final String dateTime;
    private final double temperature;
    private final double humidity;

    public Forecast(String dateTime, double temperature, double humidity) {
        this.dateTime = dateTime;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    public String getDateTime() {
        return dateTime;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }
}