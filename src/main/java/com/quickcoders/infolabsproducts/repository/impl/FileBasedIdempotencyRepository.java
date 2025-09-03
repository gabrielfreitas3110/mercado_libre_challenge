package com.quickcoders.infolabsproducts.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcoders.infolabsproducts.repository.IdempotencyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
@Profile("file")
public class FileBasedIdempotencyRepository implements IdempotencyRepository {
    
    private final Map<String, IdempotencyEntry> idempotencyCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final String dataFile;
    
    public FileBasedIdempotencyRepository(ObjectMapper objectMapper, 
                                        @Value("${app.data.file.idempotency:data/idempotency.json}") String dataFile) {
        this.objectMapper = objectMapper;
        this.dataFile = dataFile;
    }
    
    @PostConstruct
    public void loadData() {
        File file = new File(dataFile);
        if (file.exists()) {
            try {
                Map<String, IdempotencyEntry> loadedEntries = objectMapper.readValue(file, new TypeReference<Map<String, IdempotencyEntry>>() {});
                // Filter out expired entries during load
                loadedEntries.entrySet().removeIf(entry -> entry.getValue().isExpired());
                idempotencyCache.putAll(loadedEntries);
                log.info("Loaded {} idempotency entries from file: {}", idempotencyCache.size(), dataFile);
            } catch (IOException e) {
                log.error("Failed to load idempotency entries from file: {}", dataFile, e);
            }
        } else {
            log.info("Data file does not exist, starting with empty repository: {}", dataFile);
        }
    }
    
    @PreDestroy
    public void saveData() {
        try {
            File file = new File(dataFile);
            file.getParentFile().mkdirs();
            objectMapper.writeValue(file, idempotencyCache);
            log.info("Saved {} idempotency entries to file: {}", idempotencyCache.size(), dataFile);
        } catch (IOException e) {
            log.error("Failed to save idempotency entries to file: {}", dataFile, e);
        }
    }
    
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
    
    public static class IdempotencyEntry {
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
        
        public Instant getCreatedAt() {
            return createdAt;
        }
        
        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plusSeconds(EXPIRATION_HOURS * 3600));
        }
    }
}
