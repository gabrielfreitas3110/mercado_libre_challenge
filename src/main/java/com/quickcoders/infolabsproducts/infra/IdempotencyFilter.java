package com.quickcoders.infolabsproducts.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcoders.infolabsproducts.repository.IdempotencyRepository;
import com.quickcoders.infolabsproducts.service.MetricsService;
import com.quickcoders.infolabsproducts.web.ProblemDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;
    private final List<HttpMethod> writeMethods = Arrays.asList(
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE
    );

    public IdempotencyFilter(IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper, MetricsService metricsService) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        
        if (writeMethods.contains(method)) {
            String idempotencyKey = request.getHeader("Idempotency-Key");
            
            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                log.warn("Request rejected: Missing Idempotency-Key header for {} {}", method, request.getRequestURI());
                sendMissingIdempotencyKeyResponse(response);
                return;
            }
            
            if (idempotencyKey.length() > 128) {
                log.warn("Request rejected: Idempotency-Key too long ({} chars) for {} {}", 
                        idempotencyKey.length(), method, request.getRequestURI());
                sendInvalidIdempotencyKeyResponse(response, "Idempotency-Key must be 128 characters or less");
                return;
            }
            
            if (idempotencyRepository.existsByKey(idempotencyKey)) {
                log.info("Idempotent request detected for key: {}", idempotencyKey);
                String previousResult = idempotencyRepository.findByKey(idempotencyKey).orElse(null);
                if (previousResult != null) {
                    metricsService.incrementIdempotentHits();
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(previousResult);
                    return;
                }
            }
            
            // Wrap response to capture the response body
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            
            try {
                filterChain.doFilter(request, responseWrapper);
                
                // Capture response and save to idempotency store
                byte[] responseBody = responseWrapper.getContentAsByteArray();
                if (responseBody.length > 0) {
                    String responseContent = new String(responseBody);
                    idempotencyRepository.save(idempotencyKey, responseContent);
                    log.debug("Saved idempotent result for key: {}", idempotencyKey);
                }
                
                // Copy response back to original response
                responseWrapper.copyBodyToResponse();
                
            } catch (Exception e) {
                log.error("Error processing request with idempotency key: {}", idempotencyKey, e);
                throw e;
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
    
    private void sendMissingIdempotencyKeyResponse(HttpServletResponse response) throws IOException {
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("https://api.example.com/problems/missing-idempotency-key"))
                .title("Missing Idempotency Key")
                .status(400)
                .detail("Idempotency-Key header is required for write operations")
                .instance(URI.create("/api"))
                .build();
        
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
    
    private void sendInvalidIdempotencyKeyResponse(HttpServletResponse response, String detail) throws IOException {
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("https://api.example.com/problems/invalid-idempotency-key"))
                .title("Invalid Idempotency Key")
                .status(400)
                .detail(detail)
                .instance(URI.create("/api"))
                .build();
        
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
