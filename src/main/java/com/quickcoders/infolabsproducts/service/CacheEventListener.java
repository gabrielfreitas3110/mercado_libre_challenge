package com.quickcoders.infolabsproducts.service;

import com.quickcoders.infolabsproducts.domain.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEventListener {
    
    private final CacheService cacheService;
    
    @EventListener
    public void handleInventoryAdjustedEvent(InventoryAdjustedEvent event) {
        log.debug("Handling InventoryAdjustedEvent for cache invalidation");
        cacheService.invalidateAllCachesForSku(event.getInventoryRecord().getSku(), event.getInventoryRecord().getStoreId());
    }
    
    @EventListener
    public void handleReservationCreatedEvent(ReservationCreatedEvent event) {
        log.debug("Handling ReservationCreatedEvent for cache invalidation");
        cacheService.invalidateAllCachesForSku(event.getReservation().getSku(), event.getReservation().getStoreId());
    }
    
    @EventListener
    public void handleReservationCommittedEvent(ReservationCommittedEvent event) {
        log.debug("Handling ReservationCommittedEvent for cache invalidation");
        cacheService.invalidateAllCachesForSku(event.getReservation().getSku(), event.getReservation().getStoreId());
    }
    
    @EventListener
    public void handleReservationReleasedEvent(ReservationReleasedEvent event) {
        log.debug("Handling ReservationReleasedEvent for cache invalidation");
        cacheService.invalidateAllCachesForSku(event.getReservation().getSku(), event.getReservation().getStoreId());
    }
    
    @EventListener
    public void handleReservationExpiredEvent(ReservationExpiredEvent event) {
        log.debug("Handling ReservationExpiredEvent for cache invalidation");
        cacheService.invalidateAllCachesForSku(event.getReservation().getSku(), event.getReservation().getStoreId());
    }
    
    @EventListener
    public void handleItemCreatedEvent(ItemCreatedEvent event) {
        log.debug("Handling ItemCreatedEvent for cache invalidation");
        cacheService.invalidateSearchCache(event.getItem().getSku());
    }
    
    @EventListener
    public void handleCacheInvalidationEvent(CacheInvalidationEvent event) {
        log.debug("Handling CacheInvalidationEvent: SKU={}, Store={}, Reason={}", 
                event.getSku(), event.getStoreId(), event.getReason());
        cacheService.invalidateAllCachesForSku(event.getSku(), event.getStoreId());
    }
}
