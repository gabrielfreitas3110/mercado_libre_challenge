package com.quickcoders.infolabsproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Adjustment {
    private String sku;
    private String storeId;
    private Long delta;
    private Long expectedVersion;
}
