package com.quickcoders.infolabsproducts.infra.jpa.repository;

import com.quickcoders.infolabsproducts.domain.Item;
import com.quickcoders.infolabsproducts.infra.jpa.entity.ItemEntity;
import com.quickcoders.infolabsproducts.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("local")
@RequiredArgsConstructor
public class JpaItemRepositoryAdapter implements ItemRepository {
    
    private final com.quickcoders.infolabsproducts.infra.jpa.repository.ItemRepository jpaItemRepository;
    
    @Override
    public Item save(Item item) {
        ItemEntity entity = ItemEntity.builder()
                .sku(item.getSku())
                .name(item.getName())
                .attributes(item.getAttributes())
                .build();
        
        ItemEntity savedEntity = jpaItemRepository.save(entity);
        
        return Item.builder()
                .sku(savedEntity.getSku())
                .name(savedEntity.getName())
                .attributes(savedEntity.getAttributes())
                .build();
    }
    
    @Override
    public Optional<Item> findBySku(String sku) {
        return jpaItemRepository.findBySku(sku)
                .map(entity -> Item.builder()
                        .sku(entity.getSku())
                        .name(entity.getName())
                        .attributes(entity.getAttributes())
                        .build());
    }
    
    @Override
    public List<Item> findAll() {
        return jpaItemRepository.findAll().stream()
                .map(entity -> Item.builder()
                        .sku(entity.getSku())
                        .name(entity.getName())
                        .attributes(entity.getAttributes())
                        .build())
                .toList();
    }
    
    @Override
    public List<Item> findByQuery(String query, String storeId, int page, int pageSize) {
        // Simplified implementation - in real scenario would use proper pagination
        return jpaItemRepository.findByNameContainingIgnoreCase(query).stream()
                .map(entity -> Item.builder()
                        .sku(entity.getSku())
                        .name(entity.getName())
                        .attributes(entity.getAttributes())
                        .build())
                .toList();
    }
    
    @Override
    public boolean existsBySku(String sku) {
        return jpaItemRepository.existsBySku(sku);
    }
    
    @Override
    public void deleteBySku(String sku) {
        jpaItemRepository.deleteBySku(sku);
    }
}

