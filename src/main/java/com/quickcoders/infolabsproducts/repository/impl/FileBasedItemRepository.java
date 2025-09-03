package com.quickcoders.infolabsproducts.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcoders.infolabsproducts.domain.Item;
import com.quickcoders.infolabsproducts.repository.ItemRepository;
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
public class FileBasedItemRepository implements ItemRepository {
    
    private final Map<String, Item> items = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final String dataFile;
    
    public FileBasedItemRepository(ObjectMapper objectMapper, 
                                 @Value("${app.data.file.items:data/items.json}") String dataFile) {
        this.objectMapper = objectMapper;
        this.dataFile = dataFile;
    }
    
    @PostConstruct
    public void loadData() {
        File file = new File(dataFile);
        if (file.exists()) {
            try {
                Map<String, Item> loadedItems = objectMapper.readValue(file, new TypeReference<Map<String, Item>>() {});
                items.putAll(loadedItems);
                log.info("Loaded {} items from file: {}", items.size(), dataFile);
            } catch (IOException e) {
                log.error("Failed to load items from file: {}", dataFile, e);
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
            objectMapper.writeValue(file, items);
            log.info("Saved {} items to file: {}", items.size(), dataFile);
        } catch (IOException e) {
            log.error("Failed to save items to file: {}", dataFile, e);
        }
    }

    @Override
    public Item save(Item item) {
        items.put(item.getSku(), item);
        return item;
    }

    @Override
    public Optional<Item> findBySku(String sku) {
        return Optional.ofNullable(items.get(sku));
    }

    @Override
    public List<Item> findAll() {
        return List.copyOf(items.values());
    }

    @Override
    public List<Item> findByQuery(String query, String storeId, int page, int pageSize) {
        return items.values().stream()
                .filter(item -> matchesQuery(item, query))
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySku(String sku) {
        return items.containsKey(sku);
    }

    @Override
    public void deleteBySku(String sku) {
        items.remove(sku);
    }

    private boolean matchesQuery(Item item, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        
        String lowerQuery = query.toLowerCase();
        return item.getName().toLowerCase().contains(lowerQuery) ||
               item.getSku().toLowerCase().contains(lowerQuery) ||
               item.getAttributes().values().stream()
                   .anyMatch(value -> value.toLowerCase().contains(lowerQuery));
    }
}
