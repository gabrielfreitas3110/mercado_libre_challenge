package com.quickcoders.infolabsproducts.infra;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class IdempotencyStore {
    
    private final Cache<String, String> idempotencyCache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

    public boolean isProcessed(String idempotencyKey) {
        return idempotencyCache.getIfPresent(idempotencyKey) != null;
    }

    public void markAsProcessed(String idempotencyKey, String result) {
        idempotencyCache.put(idempotencyKey, result);
    }

    public String getResult(String idempotencyKey) {
        return idempotencyCache.getIfPresent(idempotencyKey);
    }
}
