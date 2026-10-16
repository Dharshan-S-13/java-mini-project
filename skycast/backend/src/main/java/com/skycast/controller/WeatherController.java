package com.skycast.controller;

import com.skycast.model.Forecast;
import com.skycast.model.Weather;
import com.skycast.service.WeatherService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Weather> getWeather(@RequestParam String city) {
        return ResponseEntity.ok(weatherService.getWeather(city));
    }

    @GetMapping("/api/forecast")
    public ResponseEntity<Forecast> getForecast(@RequestParam String city) {
        return ResponseEntity.ok(weatherService.getForecast(city));
    }
}
