package com.quickcoders.infolabsproducts.repository.impl;

import com.quickcoders.infolabsproducts.domain.InventoryRecord;
import com.quickcoders.infolabsproducts.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Profile("!file")
public class InMemoryInventoryRepository implements InventoryRepository {
    
    private final Map<String, InventoryRecord> inventoryRecords = new ConcurrentHashMap<>();

    @Override
    public InventoryRecord save(InventoryRecord record) {
        String key = generateKey(record.getSku(), record.getStoreId());
        inventoryRecords.put(key, record);
        return record;
    }

    @Override
    public Optional<InventoryRecord> findBySkuAndStoreId(String sku, String storeId) {
        String key = generateKey(sku, storeId);
        return Optional.ofNullable(inventoryRecords.get(key));
    }

    @Override
    public List<InventoryRecord> findBySku(String sku) {
        return inventoryRecords.values().stream()
                .filter(record -> record.getSku().equals(sku))
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryRecord> findBySkus(List<String> skus, String storeId) {
        return skus.stream()
                .map(sku -> findBySkuAndStoreId(sku, storeId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySkuAndStoreId(String sku, String storeId) {
        String key = generateKey(sku, storeId);
        return inventoryRecords.containsKey(key);
    }

    @Override
    public void deleteBySkuAndStoreId(String sku, String storeId) {
        String key = generateKey(sku, storeId);
        inventoryRecords.remove(key);
    }

    private String generateKey(String sku, String storeId) {
        return sku + ":" + storeId;
    }
}
