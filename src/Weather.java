public class Weather {

    private String city;
    private double temperature;
    private double humidity;
    private double windSpeed;
    private String description;

    public Weather(String city, double temperature, double humidity,
                   double windSpeed, String description) {
        this.city = city;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.description = description;
    }

    public String getCity() {
        return city;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Weather{" +
                "city='" + city + '\'' +
                ", temperature=" + temperature +
                "°C, humidity=" + humidity +
                "%, windSpeed=" + windSpeed +
                " km/h, description='" + description + '\'' +
                '}';
    }
}