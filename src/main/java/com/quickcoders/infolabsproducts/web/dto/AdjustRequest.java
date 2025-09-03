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
public class AdjustRequest {
    @NotNull(message = "Store ID is required")
    private String storeId;
    
    @NotNull(message = "Delta is required")
    private Long delta;
    
    @NotNull(message = "Expected version is required")
    @Min(value = 1, message = "Expected version must be at least 1")
    private Long expectedVersion;
}
