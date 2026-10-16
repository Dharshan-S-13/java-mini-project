package com.skycast.model;

import java.time.Instant;

public record ApiError(String error, int status, Instant timestamp) {
}
