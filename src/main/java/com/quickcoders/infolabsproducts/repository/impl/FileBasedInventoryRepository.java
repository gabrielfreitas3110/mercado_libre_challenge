package com.quickcoders.infolabsproducts.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcoders.infolabsproducts.domain.InventoryRecord;
import com.quickcoders.infolabsproducts.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Profile("file")
public class FileBasedInventoryRepository implements InventoryRepository {
    
    private final Map<String, InventoryRecord> inventoryRecords = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final String dataFile;
    
    public FileBasedInventoryRepository(ObjectMapper objectMapper, 
                                      @Value("${app.data.file.inventory:data/inventory.json}") String dataFile) {
        this.objectMapper = objectMapper;
        this.dataFile = dataFile;
    }
    
    @PostConstruct
    public void loadData() {
        File file = new File(dataFile);
        if (file.exists()) {
            try {
                Map<String, InventoryRecord> loadedRecords = objectMapper.readValue(file, new TypeReference<Map<String, InventoryRecord>>() {});
                inventoryRecords.putAll(loadedRecords);
                log.info("Loaded {} inventory records from file: {}", inventoryRecords.size(), dataFile);
            } catch (IOException e) {
                log.error("Failed to load inventory records from file: {}", dataFile, e);
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
            objectMapper.writeValue(file, inventoryRecords);
            log.info("Saved {} inventory records to file: {}", inventoryRecords.size(), dataFile);
        } catch (IOException e) {
            log.error("Failed to save inventory records to file: {}", dataFile, e);
        }
    }

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
