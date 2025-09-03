package com.quickcoders.infolabsproducts.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustRequest {
    private String storeId;
    
    @NotNull(message = "Delta is required")
    private Long delta;
    
    private Long expectedVersion;
}
