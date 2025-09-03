package com.quickcoders.infolabsproducts.infra.jpa.repository;

import com.quickcoders.infolabsproducts.infra.jpa.entity.InventoryRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRecordRepository extends JpaRepository<InventoryRecordEntity, Long> {
    
    Optional<InventoryRecordEntity> findBySkuAndStoreId(String sku, String storeId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ir FROM InventoryRecordEntity ir WHERE ir.sku = :sku AND ir.storeId = :storeId")
    Optional<InventoryRecordEntity> findBySkuAndStoreIdWithLock(@Param("sku") String sku, @Param("storeId") String storeId);
    
    List<InventoryRecordEntity> findBySku(String sku);
    
    List<InventoryRecordEntity> findByStoreId(String storeId);
    
    @Query("SELECT ir FROM InventoryRecordEntity ir WHERE ir.sku = :sku AND ir.quantityAvailable > 0")
    List<InventoryRecordEntity> findAvailableBySku(@Param("sku") String sku);
    
    @Modifying
    @Query("UPDATE InventoryRecordEntity ir SET ir.quantityAvailable = ir.quantityAvailable + :delta, ir.reserved = ir.reserved + :reservedDelta, ir.version = ir.version + 1 WHERE ir.sku = :sku AND ir.storeId = :storeId AND ir.version = :expectedVersion")
    int updateInventoryWithVersion(@Param("sku") String sku, @Param("storeId") String storeId, 
                                   @Param("delta") Long delta, @Param("reservedDelta") Long reservedDelta, 
                                   @Param("expectedVersion") Long expectedVersion);
}
