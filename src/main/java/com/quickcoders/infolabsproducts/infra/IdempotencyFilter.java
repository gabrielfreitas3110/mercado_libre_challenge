package com.quickcoders.infolabsproducts.infra;

import com.quickcoders.infolabsproducts.repository.IdempotencyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyRepository idempotencyRepository;
    private final List<HttpMethod> writeMethods = Arrays.asList(
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE
    );

    public IdempotencyFilter(IdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        
        if (writeMethods.contains(method)) {
            String idempotencyKey = request.getHeader("Idempotency-Key");
            
            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\":\"Idempotency-Key header is required for write operations\"}");
                return;
            }
            
            if (idempotencyRepository.existsByKey(idempotencyKey)) {
                log.info("Idempotent request detected for key: {}", idempotencyKey);
                String previousResult = idempotencyRepository.findByKey(idempotencyKey).orElse(null);
                if (previousResult != null) {
                    response.setContentType("application/json");
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
}
