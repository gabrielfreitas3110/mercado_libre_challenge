package com.quickcoders.infolabsproducts.config;

import com.quickcoders.infolabsproducts.service.CacheService;
import com.quickcoders.infolabsproducts.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {

    private final InventoryService inventoryService;
    private final CacheService cacheService;

    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanupExpiredReservations() {
        log.info("Cleaning up expired reservations");
        inventoryService.expireReservations();
    }
    
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void logCacheStatistics() {
        log.info("Logging cache statistics");
        cacheService.logCacheStats();
    }
}
