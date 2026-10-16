package com.skycast.model;

public record Weather(
        String city,
        String country,
        long timestamp,
        int timezone,
        double temperature,
        double feelsLike,
        double humidity,
        double windSpeed,
        double pressure,
        String description,
        String icon) {
}
