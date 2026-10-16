package com.skycast.controller;

import com.skycast.model.ApiError;
import com.skycast.service.WeatherApiException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(WeatherApiException.class)
    public ResponseEntity<ApiError> handleApiException(WeatherApiException exception) {
        return response(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiError> handleInvalidRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "Provide a city or a valid chat request.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred.");
    }

    private static ResponseEntity<ApiError> response(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(message, status.value(), Instant.now()));
    }
}
