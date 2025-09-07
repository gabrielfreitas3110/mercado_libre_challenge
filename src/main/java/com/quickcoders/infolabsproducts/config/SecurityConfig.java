package com.quickcoders.infolabsproducts.config;

import com.quickcoders.infolabsproducts.infra.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.security.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    @Value("${app.security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.security.cors.allowed-headers:Authorization,Content-Type,X-Correlation-Id,Idempotency-Key}")
    private String allowedHeaders;

    @Value("${app.security.cors.allow-credentials:true}")
    private boolean allowCredentials;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Profile({"dev", "local"})
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Permitir acesso público à documentação em dev/local
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                // Permitir acesso público ao actuator
                .requestMatchers("/actuator/**").permitAll()
                // Permitir acesso público ao OpenAPI JSON
                .requestMatchers("/openapi.yaml", "/openapi.json").permitAll()
                // Em dev/local, permitir acesso sem autenticação para todas as rotas
                .anyRequest().permitAll()
            )
            .httpBasic(AbstractHttpConfigurer::disable);
        
        return http.build();
    }

    @Bean
    @Profile("prod")
    public SecurityFilterChain prodFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(authz -> authz
                // Em prod, apenas health e prometheus são públicos
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                // Todas as outras requisições precisam de autenticação
                .anyRequest().authenticated()
            )
            .httpBasic(AbstractHttpConfigurer::disable);
        
        return http.build();
    }

    @Bean
    @Profile("test")
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .httpBasic(AbstractHttpConfigurer::disable);
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Configurar origens permitidas
        if ("*".equals(allowedOrigins)) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        }
        
        // Configurar métodos permitidos
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        
        // Configurar headers permitidos
        if ("*".equals(allowedHeaders)) {
            configuration.setAllowedHeaders(List.of("*"));
        } else {
            configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        }
        
        configuration.setAllowCredentials(allowCredentials);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
