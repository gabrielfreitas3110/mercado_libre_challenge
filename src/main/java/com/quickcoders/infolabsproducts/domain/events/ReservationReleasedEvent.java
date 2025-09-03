package com.quickcoders.infolabsproducts.domain.events;

import com.quickcoders.infolabsproducts.domain.Reservation;
import lombok.Getter;

import java.time.Instant;

@Getter
public class ReservationReleasedEvent implements DomainEvent {
    private final Reservation reservation;
    private final Instant timestamp;
    private final String eventType = "ReservationReleased";

    public ReservationReleasedEvent(Reservation reservation) {
        this.reservation = reservation;
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
