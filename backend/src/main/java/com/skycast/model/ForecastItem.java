package com.skycast.model;

public record ForecastItem(
        long timestamp,
        String dateTime,
        double temperature,
        double minTemperature,
        double maxTemperature,
        double humidity,
        double precipitation,
        String description,
        String icon) {
}
