package com.quickcoders.infolabsproducts.repository.impl;

import com.quickcoders.infolabsproducts.domain.Item;
import com.quickcoders.infolabsproducts.repository.ItemRepository;
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
@Profile("inmemory")
public class InMemoryItemRepository implements ItemRepository {
    
    private final Map<String, Item> items = new ConcurrentHashMap<>();

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
               (item.getAttributes() != null && item.getAttributes().values().stream()
                   .anyMatch(value -> value.toLowerCase().contains(lowerQuery)));
    }
}