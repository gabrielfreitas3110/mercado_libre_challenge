package com.quickcoders.infolabsproducts.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateItemRequest {
    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must be at most 50 characters")
    private String sku;
    
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    private String name;
    
    private Map<String, String> attributes;
    
    private Long quantityAvailable;
}
