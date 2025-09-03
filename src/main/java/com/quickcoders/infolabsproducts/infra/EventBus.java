package com.quickcoders.infolabsproducts.infra;

import com.quickcoders.infolabsproducts.domain.events.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventBus {
    
    private final ApplicationEventPublisher publisher;

    public EventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(DomainEvent event) {
        log.info("Publishing event: {} at {}", event.getEventType(), event.getTimestamp());
        publisher.publishEvent(event);
    }
}
