package com.relay.shared.web;

import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(EntityNotFoundException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> badRequest(RuntimeException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(com.relay.auth.AuthService.EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> notVerified(com.relay.auth.AuthService.EmailNotVerifiedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "timestamp", OffsetDateTime.now().toString(),
            "status", 403,
            "error", "Forbidden",
            "message", e.getMessage(),
            "needsVerification", true,
            "email", e.getEmail()));
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "timestamp", OffsetDateTime.now().toString(),
            "status", status.value(),
            "error", status.getReasonPhrase(),
            "message", message == null ? "" : message));
    }
}
