package com.quickcoders.infolabsproducts.infra.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "inventory_records", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"sku", "store_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRecordEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "sku", nullable = false, length = 100)
    private String sku;
    
    @Column(name = "store_id", nullable = false, length = 100)
    private String storeId;
    
    @Column(name = "quantity_available", nullable = false)
    private Long quantityAvailable;
    
    @Column(name = "reserved", nullable = false)
    private Long reserved;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
