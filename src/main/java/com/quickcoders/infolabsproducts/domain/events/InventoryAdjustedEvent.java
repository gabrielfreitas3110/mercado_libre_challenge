package com.quickcoders.infolabsproducts.domain.events;

import com.quickcoders.infolabsproducts.domain.InventoryRecord;
import lombok.Getter;

import java.time.Instant;

@Getter
public class InventoryAdjustedEvent implements DomainEvent {
    private final InventoryRecord inventoryRecord;
    private final Long delta;
    private final Instant timestamp;
    private final String eventType = "InventoryAdjusted";

    public InventoryAdjustedEvent(InventoryRecord inventoryRecord, Long delta) {
        this.inventoryRecord = inventoryRecord;
        this.delta = delta;
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
