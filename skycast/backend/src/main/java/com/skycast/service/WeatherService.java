package com.skycast.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycast.model.Forecast;
import com.skycast.model.Weather;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {
    private final ObjectMapper objectMapper;

    public WeatherService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Weather getWeather(String city) {
        // Placeholder: migrate existing logic in Task 2
        return new Weather(city, "", 0L, 0, 0.0, 0.0, 0.0, 0.0, 0.0, "", "");
    }

    public Forecast getForecast(String city) {
        // Placeholder: migrate existing logic in Task 2
        return new Forecast(city, "", 0L, 0, java.util.List.of());
    }
}
