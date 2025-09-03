package com.quickcoders.infolabsproducts.infra;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class IdempotencyStore {
    
    private final Cache<String, String> idempotencyCache;
    
    public IdempotencyStore(@Value("${inventory.idempotency.ttl:PT24H}") String ttl) {
        Duration duration = Duration.parse(ttl);
        long hours = duration.toHours();
        
        log.info("Initializing IdempotencyStore with TTL: {} ({} hours)", ttl, hours);
        
        this.idempotencyCache = Caffeine.newBuilder()
                .expireAfterWrite(hours, TimeUnit.HOURS)
                .maximumSize(10000)
                .build();
    }

    public boolean isProcessed(String idempotencyKey) {
        return idempotencyCache.getIfPresent(idempotencyKey) != null;
    }

    public void markAsProcessed(String idempotencyKey, String result) {
        idempotencyCache.put(idempotencyKey, result);
        log.debug("Marked idempotency key as processed: {}", idempotencyKey);
    }

    public String getResult(String idempotencyKey) {
        String result = idempotencyCache.getIfPresent(idempotencyKey);
        if (result != null) {
            log.debug("Retrieved idempotent result for key: {}", idempotencyKey);
        }
        return result;
    }
}
