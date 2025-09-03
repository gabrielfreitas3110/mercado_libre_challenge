package com.quickcoders.infolabsproducts.repository.impl;

import com.quickcoders.infolabsproducts.repository.IdempotencyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
@Profile("!file")
public class InMemoryIdempotencyRepository implements IdempotencyRepository {
    
    private final Map<String, IdempotencyEntry> idempotencyCache = new ConcurrentHashMap<>();
    
    @Override
    public void save(String idempotencyKey, String result) {
        idempotencyCache.put(idempotencyKey, new IdempotencyEntry(result, Instant.now()));
    }

    @Override
    public Optional<String> findByKey(String idempotencyKey) {
        IdempotencyEntry entry = idempotencyCache.get(idempotencyKey);
        if (entry != null && !entry.isExpired()) {
            return Optional.of(entry.getResult());
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByKey(String idempotencyKey) {
        IdempotencyEntry entry = idempotencyCache.get(idempotencyKey);
        return entry != null && !entry.isExpired();
    }

    @Override
    public void deleteByKey(String idempotencyKey) {
        idempotencyCache.remove(idempotencyKey);
    }

    @Override
    public void deleteExpired() {
        idempotencyCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    private static class IdempotencyEntry {
        private final String result;
        private final Instant createdAt;
        private static final long EXPIRATION_HOURS = 24;
        
        public IdempotencyEntry(String result, Instant createdAt) {
            this.result = result;
            this.createdAt = createdAt;
        }
        
        public String getResult() {
            return result;
        }
        
        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plusSeconds(EXPIRATION_HOURS * 3600));
        }
    }
}
