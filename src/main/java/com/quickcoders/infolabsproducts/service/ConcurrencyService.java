package com.quickcoders.infolabsproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Slf4j
@Service
public class ConcurrencyService {
    
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    
    public <T> T executeWithLock(String sku, String storeId, Supplier<T> operation) {
        String lockKey = generateLockKey(sku, storeId);
        ReentrantLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        
        try {
            lock.lock();
            log.debug("Acquired lock for key: {}", lockKey);
            return operation.get();
        } finally {
            lock.unlock();
            log.debug("Released lock for key: {}", lockKey);
        }
    }
    
    public void executeWithLock(String sku, String storeId, Runnable operation) {
        String lockKey = generateLockKey(sku, storeId);
        ReentrantLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        
        try {
            lock.lock();
            log.debug("Acquired lock for key: {}", lockKey);
            operation.run();
        } finally {
            lock.unlock();
            log.debug("Released lock for key: {}", lockKey);
        }
    }
    
    private String generateLockKey(String sku, String storeId) {
        return sku + "#" + storeId;
    }
}
