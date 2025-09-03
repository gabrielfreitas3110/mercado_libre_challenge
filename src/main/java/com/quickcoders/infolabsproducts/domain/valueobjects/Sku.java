package com.quickcoders.infolabsproducts.domain.valueobjects;

import lombok.Value;

@Value
public class Sku {
    String value;

    public Sku(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU cannot be null or empty");
        }
        this.value = value.trim();
    }

    @Override
    public String toString() {
        return value;
    }
}
