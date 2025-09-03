package com.quickcoders.infolabsproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRecord {
    private String sku;
    private String storeId;
    private Long quantityAvailable;
    private Long reserved;
    private Long version;
    private Instant updatedAt;
}
