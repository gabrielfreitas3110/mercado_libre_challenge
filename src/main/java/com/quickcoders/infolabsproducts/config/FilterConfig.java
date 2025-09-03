package com.quickcoders.infolabsproducts.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcoders.infolabsproducts.infra.CorrelationFilter;
import com.quickcoders.infolabsproducts.infra.IdempotencyFilter;
import com.quickcoders.infolabsproducts.web.IdempotencyKeyValidator;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FilterConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<CorrelationFilter> correlationFilterRegistration(ObjectMapper objectMapper) {
        FilterRegistrationBean<CorrelationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationFilter(objectMapper));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration(IdempotencyFilter idempotencyFilter) {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(idempotencyFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(2);
        return registration;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new IdempotencyKeyValidator())
                .addPathPatterns("/items/**", "/availability/**")
                .excludePathPatterns("/items", "/items/*", "/availability"); // Exclude GET operations
    }
}
