package com.quickcoders.infolabsproducts.infra.jpa.repository;

import com.quickcoders.infolabsproducts.infra.jpa.entity.ItemEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, String> {
    
    Optional<ItemEntity> findBySku(String sku);
    
    @Query("SELECT i FROM ItemEntity i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<ItemEntity> findByNameContainingIgnoreCase(@Param("name") String name);
    
    @Query("SELECT i FROM ItemEntity i JOIN i.attributes a WHERE LOWER(KEY(a)) = LOWER(:key) AND LOWER(VALUE(a)) LIKE LOWER(CONCAT('%', :value, '%'))")
    List<ItemEntity> findByAttributeKeyAndValueContaining(@Param("key") String key, @Param("value") String value);
    
    boolean existsBySku(String sku);
    
    void deleteBySku(String sku);
}
