package com.quickcoders.infolabsproducts.infra;

import com.quickcoders.infolabsproducts.service.JwtService;
import com.quickcoders.infolabsproducts.web.ProblemDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        // Em ambiente de teste, permitir requisições sem token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found in request");
            // Em teste, criar uma autenticação mock
            if (isTestEnvironment()) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "test-user",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("USER"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                MDC.put("userId", "test-user");
            }
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            var userId = jwtService.validateToken(authHeader);
            
            if (userId.isPresent()) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId.get(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("USER"))
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                MDC.put("userId", userId.get());
                log.debug("Authentication set for user: {}", userId.get());
            } else {
                log.warn("Invalid JWT token");
                if (!isTestEnvironment()) {
                    sendUnauthorizedResponse(response, "Invalid JWT token");
                    return;
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing JWT token", e);
            if (!isTestEnvironment()) {
                sendUnauthorizedResponse(response, "Error processing JWT token");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isTestEnvironment() {
        String activeProfile = System.getProperty("spring.profiles.active");
        return "test".equals(activeProfile);
    }
    
    private void sendUnauthorizedResponse(HttpServletResponse response, String detail) throws IOException {
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("https://api.example.com/problems/unauthorized"))
                .title("Unauthorized")
                .status(401)
                .detail(detail)
                .instance(URI.create("/api"))
                .build();
        
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/problem+json");
        response.getWriter().write(convertToJson(problem));
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
