package com.quickcoders.infolabsproducts.service;

import com.quickcoders.infolabsproducts.domain.Item;
import com.quickcoders.infolabsproducts.domain.events.ItemCreatedEvent;
import com.quickcoders.infolabsproducts.infra.EventBus;
import com.quickcoders.infolabsproducts.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {
    
    private final ItemRepository itemRepository;
    private final EventBus eventBus;

    public Item createItem(Item item) {
        log.info("Creating item with SKU: {}", item.getSku());
        
        if (itemRepository.existsBySku(item.getSku())) {
            throw new IllegalArgumentException("Item with SKU " + item.getSku() + " already exists");
        }
        
        Item savedItem = itemRepository.save(item);
        eventBus.publish(new ItemCreatedEvent(savedItem));
        
        return savedItem;
    }

    public Item getItem(String sku) {
        return itemRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Item with SKU " + sku + " not found"));
    }

    public List<Item> searchItems(String query, String storeId, int page, int pageSize) {
        return itemRepository.findByQuery(query, storeId, page, pageSize);
    }

    public boolean itemExists(String sku) {
        return itemRepository.existsBySku(sku);
    }
}
