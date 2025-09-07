package com.quickcoders.infolabsproducts.infra.jpa.repository;

import com.quickcoders.infolabsproducts.domain.InventoryRecord;
import com.quickcoders.infolabsproducts.infra.jpa.entity.InventoryRecordEntity;
import com.quickcoders.infolabsproducts.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("local")
@RequiredArgsConstructor
public class JpaInventoryRepositoryAdapter implements InventoryRepository {
    
    private final InventoryRecordRepository jpaInventoryRepository;
    
    @Override
    public InventoryRecord save(InventoryRecord record) {
        InventoryRecordEntity entity = InventoryRecordEntity.builder()
                .id(record.getId())
                .sku(record.getSku())
                .storeId(record.getStoreId())
                .quantityAvailable(record.getQuantityAvailable())
                .reserved(record.getReserved())
                .version(record.getVersion())
                .build();
        
        InventoryRecordEntity savedEntity = jpaInventoryRepository.save(entity);
        
        return InventoryRecord.builder()
                .id(savedEntity.getId())
                .sku(savedEntity.getSku())
                .storeId(savedEntity.getStoreId())
                .quantityAvailable(savedEntity.getQuantityAvailable())
                .reserved(savedEntity.getReserved())
                .version(savedEntity.getVersion())
                .updatedAt(savedEntity.getUpdatedAt())
                .build();
    }
    
    @Override
    public Optional<InventoryRecord> findBySkuAndStoreId(String sku, String storeId) {
        return jpaInventoryRepository.findBySkuAndStoreId(sku, storeId)
                .map(this::toDomain);
    }
    
    @Override
    public List<InventoryRecord> findBySku(String sku) {
        return jpaInventoryRepository.findBySku(sku).stream()
                .map(this::toDomain)
                .toList();
    }
    
    @Override
    public List<InventoryRecord> findBySkus(List<String> skus, String storeId) {
        return jpaInventoryRepository.findByStoreId(storeId).stream()
                .filter(entity -> skus.contains(entity.getSku()))
                .map(this::toDomain)
                .toList();
    }
    
    @Override
    public boolean existsBySkuAndStoreId(String sku, String storeId) {
        return jpaInventoryRepository.findBySkuAndStoreId(sku, storeId).isPresent();
    }

    @Override
    public void deleteBySkuAndStoreId(String sku, String storeId) {
        jpaInventoryRepository.findBySkuAndStoreId(sku, storeId)
                .ifPresent(entity -> jpaInventoryRepository.deleteById(entity.getId()));
    }
    
    private InventoryRecord toDomain(InventoryRecordEntity entity) {
        return InventoryRecord.builder()
                .id(entity.getId())
                .sku(entity.getSku())
                .storeId(entity.getStoreId())
                .quantityAvailable(entity.getQuantityAvailable())
                .reserved(entity.getReserved())
                .version(entity.getVersion())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
