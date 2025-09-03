package com.quickcoders.infolabsproducts.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class IdempotencyKeyValidator implements HandlerInterceptor {
    
    private static final List<String> MUTATION_METHODS = Arrays.asList("POST", "PUT", "PATCH", "DELETE");
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        
        if (MUTATION_METHODS.contains(method)) {
            String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
            
            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                log.warn("Missing Idempotency-Key header for mutation request: {} {}", method, request.getRequestURI());
                
                ProblemDetails problem = ProblemDetails.builder()
                        .type(URI.create("https://api.example.com/problems/missing-idempotency-key"))
                        .title("Missing Idempotency Key")
                        .status(400)
                        .detail("Idempotency-Key header is required for mutation operations")
                        .instance(URI.create(request.getRequestURI()))
                        .build();
                
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setContentType("application/problem+json");
                response.getWriter().write(convertToJson(problem));
                return false;
            }
            
            if (idempotencyKey.length() > 128) {
                log.warn("Idempotency-Key too long: {} characters", idempotencyKey.length());
                
                ProblemDetails problem = ProblemDetails.builder()
                        .type(URI.create("https://api.example.com/problems/invalid-idempotency-key"))
                        .title("Invalid Idempotency Key")
                        .status(400)
                        .detail("Idempotency-Key must be at most 128 characters")
                        .instance(URI.create(request.getRequestURI()))
                        .build();
                
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setContentType("application/problem+json");
                response.getWriter().write(convertToJson(problem));
                return false;
            }
        }
        
        return true;
    }
    
    private String convertToJson(ProblemDetails problem) {
        return String.format(
            "{\"type\":\"%s\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"instance\":\"%s\"}",
            problem.getType(),
            problem.getTitle(),
            problem.getStatus(),
            problem.getDetail(),
            problem.getInstance()
        );
    }
}
