package com.quickcoders.infolabsproducts.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class CorrelationFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String USER_ID_MDC_KEY = "userId";
    private static final String REQUEST_START_TIME_MDC_KEY = "requestStartTime";
    
    private final ObjectMapper objectMapper;

    public CorrelationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        long startTime = System.currentTimeMillis();
        
        // Set MDC for structured logging
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        MDC.put(REQUEST_START_TIME_MDC_KEY, String.valueOf(startTime));
        
        // Set response header
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            // Log request start with structured JSON
            logRequestStart(request, correlationId, startTime);
            
            filterChain.doFilter(request, response);
            
            // Log request completion with structured JSON
            logRequestCompletion(request, response, correlationId, startTime);
            
        } finally {
            MDC.clear();
        }
    }
    
    private void logRequestStart(HttpServletRequest request, String correlationId, long startTime) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("event", "request_start");
            logData.put("correlationId", correlationId);
            logData.put("method", request.getMethod());
            logData.put("uri", request.getRequestURI());
            logData.put("queryString", request.getQueryString());
            logData.put("userAgent", request.getHeader("User-Agent"));
            logData.put("remoteAddr", request.getRemoteAddr());
            logData.put("timestamp", Instant.now().toString());
            logData.put("startTime", startTime);
            
            String jsonLog = objectMapper.writeValueAsString(logData);
            log.info("Request started: {}", jsonLog);
            
        } catch (Exception e) {
            log.warn("Failed to log request start: {}", e.getMessage());
        }
    }
    
    private void logRequestCompletion(HttpServletRequest request, HttpServletResponse response, 
                                    String correlationId, long startTime) {
        try {
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> logData = new HashMap<>();
            logData.put("event", "request_completed");
            logData.put("correlationId", correlationId);
            logData.put("method", request.getMethod());
            logData.put("uri", request.getRequestURI());
            logData.put("status", response.getStatus());
            logData.put("duration", duration);
            logData.put("timestamp", Instant.now().toString());
            logData.put("contentType", response.getContentType());
            
            // Add user ID if available from security context
            String userId = MDC.get(USER_ID_MDC_KEY);
            if (userId != null) {
                logData.put("userId", userId);
            }
            
            String jsonLog = objectMapper.writeValueAsString(logData);
            
            if (response.getStatus() >= 400) {
                log.warn("Request completed with error: {}", jsonLog);
            } else {
                log.info("Request completed: {}", jsonLog);
            }
            
        } catch (Exception e) {
            log.warn("Failed to log request completion: {}", e.getMessage());
        }
    }
}
