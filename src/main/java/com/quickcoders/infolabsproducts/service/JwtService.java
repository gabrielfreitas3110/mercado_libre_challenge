package com.quickcoders.infolabsproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class JwtService {
    
    @Value("${app.security.jwt.enabled:true}")
    private boolean jwtEnabled;
    
    @Value("${app.security.jwt.secret:local-secret-key}")
    private String jwtSecret;
    
    public Optional<String> validateToken(String token) {
        if (!jwtEnabled) {
            log.debug("JWT validation disabled, accepting all tokens");
            return Optional.of("local-user");
        }
        
        if (token == null || token.trim().isEmpty()) {
            log.warn("Empty or null JWT token");
            return Optional.empty();
        }
        
        if (!token.startsWith("Bearer ")) {
            log.warn("Invalid JWT token format: missing 'Bearer ' prefix");
            return Optional.empty();
        }
        
        String actualToken = token.substring(7); // Remove "Bearer " prefix
        
        if (actualToken.trim().isEmpty()) {
            log.warn("Empty JWT token after removing Bearer prefix");
            return Optional.empty();
        }
        
        // Stub validation - in production this would validate the JWT signature
        if (isValidTokenFormat(actualToken)) {
            String userId = extractUserIdFromToken(actualToken);
            log.debug("JWT token validated for user: {}", userId);
            return Optional.of(userId);
        } else {
            log.warn("Invalid JWT token format");
            return Optional.empty();
        }
    }
    
    private boolean isValidTokenFormat(String token) {
        // Stub validation - check if token has the expected format (3 parts separated by dots)
        String[] parts = token.split("\\.");
        return parts.length == 3 && 
               !parts[0].isEmpty() && 
               !parts[1].isEmpty() && 
               !parts[2].isEmpty();
    }
    
    private String extractUserIdFromToken(String token) {
        // Stub extraction - in production this would decode the JWT payload
        // For now, return a hash of the token as user ID
        return "user-" + Math.abs(token.hashCode());
    }
    
    public boolean isJwtEnabled() {
        return jwtEnabled;
    }
}
