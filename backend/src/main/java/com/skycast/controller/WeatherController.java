package com.skycast.controller;

import com.skycast.model.Forecast;
import com.skycast.model.Weather;
import com.skycast.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {
    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/api/weather")
    public Weather getWeather(@RequestParam String city) {
        return weatherService.getWeather(city);
    }

    @GetMapping("/api/forecast")
    public Forecast getForecast(@RequestParam String city) {
        return weatherService.getForecast(city);
    }
}
