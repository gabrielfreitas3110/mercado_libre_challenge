package com.quickcoders.infolabsproducts.domain.valueobjects;

import lombok.Value;

@Value
public class StoreId {
    String value;
    
    public static final StoreId GLOBAL = new StoreId("GLOBAL");

    public StoreId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Store ID cannot be null or empty");
        }
        this.value = value.trim();
    }

    @Override
    public String toString() {
        return value;
    }
}
