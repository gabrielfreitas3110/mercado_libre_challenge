package com.quickcoders.infolabsproducts.domain.events;

import com.quickcoders.infolabsproducts.domain.Item;
import lombok.Getter;

import java.time.Instant;

@Getter
public class ItemCreatedEvent implements DomainEvent {
    private final Item item;
    private final Instant timestamp;
    private final String eventType = "ItemCreated";

    public ItemCreatedEvent(Item item) {
        this.item = item;
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
