package com.skycast.model;

import java.util.List;

public record Forecast(
        String city,
        String country,
        long timestamp,
        int timezone,
        List<ForecastItem> items) {
}
