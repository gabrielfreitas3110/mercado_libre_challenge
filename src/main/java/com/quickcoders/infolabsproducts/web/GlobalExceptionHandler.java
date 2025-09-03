package com.quickcoders.infolabsproducts.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetails> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument exception: {}", ex.getMessage());
        
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("https://api.example.com/problems/bad-request"))
                .title("Bad Request")
                .status(400)
                .detail(ex.getMessage())
                .instance(URI.create(request.getDescription(false)))
                .build();
        
        return ResponseEntity.badRequest()
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(problem);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetails> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state exception: {}", ex.getMessage());
        
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("https://api.example.com/problems/conflict"))
                .title("Conflict")
                .status(409)
                .detail(ex.getMessage())
                .instance(URI.create(request.getDescription(false)))
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(problem);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetails> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.warn("Authentication exception: {}", ex.getMessage());
        
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("https://api.example.com/problems/unauthorized"))
                .title("Unauthorized")
                .status(401)
                .detail("Authentication failed: " + ex.getMessage())
                .instance(URI.create(request.getDescription(false)))
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetails> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied exception: {}", ex.getMessage());
        
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("https://api.example.com/problems/forbidden"))
                .title("Forbidden")
                .status(403)
                .detail("Access denied: " + ex.getMessage())
                .instance(URI.create(request.getDescription(false)))
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation exception: {}", ex.getMessage());
        
        Map<String, String[]> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, new String[]{errorMessage});
        });
        
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("https://api.example.com/problems/validation-error"))
                .title("Validation Error")
                .status(400)
                .detail("Validation failed")
                .instance(URI.create(request.getDescription(false)))
                .errors(errors)
                .build();
        
        return ResponseEntity.badRequest()
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error: ", ex);
        
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("https://api.example.com/problems/internal-error"))
                .title("Internal Server Error")
                .status(500)
                .detail("An unexpected error occurred")
                .instance(URI.create(request.getDescription(false)))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(problem);
    }
}
