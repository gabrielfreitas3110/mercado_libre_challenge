package com.quickcoders.infolabsproducts.domain.events;

import lombok.Getter;

import java.time.Instant;

@Getter
public class CacheInvalidationEvent implements DomainEvent {
    private final String sku;
    private final String storeId;
    private final String reason;
    private final Instant timestamp;
    private final String eventType = "CacheInvalidation";

    public CacheInvalidationEvent(String sku, String storeId, String reason) {
        this.sku = sku;
        this.storeId = storeId;
        this.reason = reason;
        this.timestamp = Instant.now();
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getEventType() {
        return eventType;
    }
}
