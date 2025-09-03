package com.quickcoders.infolabsproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    private String sku;
    private String name;
    private Map<String, String> attributes;
}
