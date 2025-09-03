package com.quickcoders.infolabsproducts.repository;

import com.quickcoders.infolabsproducts.domain.InventoryRecord;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    InventoryRecord save(InventoryRecord record);
    Optional<InventoryRecord> findBySkuAndStoreId(String sku, String storeId);
    List<InventoryRecord> findBySku(String sku);
    List<InventoryRecord> findBySkus(List<String> skus, String storeId);
    boolean existsBySkuAndStoreId(String sku, String storeId);
    void deleteBySkuAndStoreId(String sku, String storeId);
}
