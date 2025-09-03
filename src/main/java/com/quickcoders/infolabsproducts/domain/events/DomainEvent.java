package com.quickcoders.infolabsproducts.domain.events;

import java.time.Instant;

public interface DomainEvent {
    Instant getTimestamp();
    String getEventType();
}
