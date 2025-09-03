package com.quickcoders.infolabsproducts.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveRequest {
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Long qty;
    
    private String storeId;
    
    @NotNull(message = "TTL is required")
    @Min(value = 1, message = "TTL must be at least 1 second")
    private Long ttlSeconds;
    
    private Long expectedVersion;
}
