
public class Forecast {

    private String dateTime;
    private double temperature;
    private double feelsLike;
    private int humidity;
    private double windSpeed;
    private String description;

    public Forecast(String dateTime, double temperature,
                    double feelsLike, int humidity,
                    double windSpeed, String description) {
        this.dateTime = dateTime;
        this.temperature = temperature;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.description = description;
    }

    public String getDateTime() {
        return dateTime;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public int getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public String getDescription() {
        return description;
    }
}