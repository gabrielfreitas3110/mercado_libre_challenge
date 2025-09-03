package com.quickcoders.infolabsproducts.repository;

import java.util.Optional;

public interface IdempotencyRepository {
    void save(String idempotencyKey, String result);
    Optional<String> findByKey(String idempotencyKey);
    boolean existsByKey(String idempotencyKey);
    void deleteByKey(String idempotencyKey);
    void deleteExpired();
}
