package com.quickcoders.infolabsproducts.service;

import com.quickcoders.infolabsproducts.domain.*;
import com.quickcoders.infolabsproducts.domain.enums.ReservationStatus;
import com.quickcoders.infolabsproducts.domain.events.*;
import com.quickcoders.infolabsproducts.infra.EventBus;
import com.quickcoders.infolabsproducts.repository.*;
import com.quickcoders.infolabsproducts.web.dto.*;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    
    private final ItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final EventBus eventBus;
    private final ConcurrencyService concurrencyService;
    private final CacheService cacheService;
    private final MetricsService metricsService;

    public Item createItem(Item item) {
        log.info("Creating item with SKU: {}", item.getSku());
        
        if (itemRepository.existsBySku(item.getSku())) {
            throw new IllegalArgumentException("Item with SKU " + item.getSku() + " already exists");
        }
        
        Item savedItem = itemRepository.save(item);
        eventBus.publish(new ItemCreatedEvent(savedItem));
        
        return savedItem;
    }

    public InventoryRecord adjust(Adjustment adjustment) {
        log.info("Adjusting inventory for SKU: {}, Store: {}, Delta: {}, Expected Version: {}", 
                adjustment.getSku(), adjustment.getStoreId(), adjustment.getDelta(), adjustment.getExpectedVersion());
        
        Timer.Sample sample = metricsService.startAdjustTimer();
        String storeId = adjustment.getStoreId() != null ? adjustment.getStoreId() : "GLOBAL";
        
        try {
            return concurrencyService.executeWithLock(adjustment.getSku(), storeId, () -> {
            InventoryRecord currentRecord = inventoryRepository.findBySkuAndStoreId(adjustment.getSku(), storeId)
                    .orElse(createInitialRecord(adjustment.getSku(), storeId));
            
            // Controle otimista
            if (adjustment.getExpectedVersion() != null && !currentRecord.getVersion().equals(adjustment.getExpectedVersion())) {
                metricsService.incrementVersionConflicts();
                metricsService.incrementOptimisticLockingConflicts();
                throw new IllegalStateException(String.format("Version conflict. Expected: %d, Actual: %d", 
                        adjustment.getExpectedVersion(), currentRecord.getVersion()));
            }
            
            long newQuantity = currentRecord.getQuantityAvailable() + adjustment.getDelta();
            if (newQuantity < 0) {
                throw new IllegalArgumentException("Insufficient inventory. Available: " + 
                        currentRecord.getQuantityAvailable() + ", Requested: " + Math.abs(adjustment.getDelta()));
            }
            
            InventoryRecord updatedRecord = InventoryRecord.builder()
                    .sku(adjustment.getSku())
                    .storeId(storeId)
                    .quantityAvailable(newQuantity)
                    .reserved(currentRecord.getReserved())
                    .version(currentRecord.getVersion() + 1)
                    .updatedAt(Instant.now())
                    .build();
            
            InventoryRecord savedRecord = inventoryRepository.save(updatedRecord);
            eventBus.publish(new InventoryAdjustedEvent(savedRecord, adjustment.getDelta()));
            
            // Publish cache invalidation event
            eventBus.publish(new CacheInvalidationEvent(adjustment.getSku(), storeId, "inventory_adjusted"));
            
            return savedRecord;
            });
        } finally {
            metricsService.recordAdjustTimer(sample);
        }
    }

    public Reservation reserve(String sku, String storeId, Long qty, Long ttlSeconds, Long expectedVersion) {
        log.info("Creating reservation for SKU: {}, Store: {}, Qty: {}, TTL: {}s, Expected Version: {}", 
                sku, storeId, qty, ttlSeconds, expectedVersion);
        
        Timer.Sample sample = metricsService.startReservationTimer();
        String targetStoreId = storeId != null ? storeId : "GLOBAL";
        
        try {
            return concurrencyService.executeWithLock(sku, targetStoreId, () -> {
            // Check inventory availability
            var inventoryRecord = inventoryRepository.findBySkuAndStoreId(sku, targetStoreId)
                    .orElseThrow(() -> new IllegalArgumentException("Inventory not found for SKU: " + sku));
            
            // Controle otimista
            if (expectedVersion != null && !inventoryRecord.getVersion().equals(expectedVersion)) {
                metricsService.incrementVersionConflicts();
                metricsService.incrementOptimisticLockingConflicts();
                throw new IllegalStateException(String.format("Version conflict. Expected: %d, Actual: %d", 
                        expectedVersion, inventoryRecord.getVersion()));
            }
            
            if (inventoryRecord.getQuantityAvailable() < qty) {
                throw new IllegalArgumentException("Insufficient inventory. Available: " + 
                        inventoryRecord.getQuantityAvailable() + ", Requested: " + qty);
            }
            
            // Create reservation
            Reservation reservation = Reservation.builder()
                    .reservationId(UUID.randomUUID().toString())
                    .sku(sku)
                    .storeId(targetStoreId)
                    .qty(qty)
                    .status(ReservationStatus.CREATED)
                    .expiresAt(Instant.now().plusSeconds(ttlSeconds))
                    .createdAt(Instant.now())
                    .build();
            
            // Update inventory (reserve quantity)
            updateInventoryReserved(inventoryRecord, qty);
            
            Reservation savedReservation = reservationRepository.save(reservation);
            eventBus.publish(new ReservationCreatedEvent(savedReservation));
            metricsService.incrementReservationsCreated();
            
            // Publish cache invalidation event
            eventBus.publish(new CacheInvalidationEvent(sku, targetStoreId, "reservation_created"));
            
            return savedReservation;
            });
        } finally {
            metricsService.recordReservationTimer(sample);
        }
    }

    public InventoryRecord commit(String reservationId, Long expectedVersion) {
        log.info("Committing reservation: {}, Expected Version: {}", reservationId, expectedVersion);
        
        Timer.Sample sample = metricsService.startCommitTimer();
        Reservation reservation = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        
        try {
            return concurrencyService.executeWithLock(reservation.getSku(), reservation.getStoreId(), () -> {
            // Re-fetch reservation to ensure it's still valid
            Reservation currentReservation = reservationRepository.findByReservationId(reservationId)
                    .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
            
            if (currentReservation.getStatus() != ReservationStatus.CREATED) {
                throw new IllegalStateException("Reservation is not in CREATED status");
            }
            
            if (currentReservation.getExpiresAt().isBefore(Instant.now())) {
                throw new IllegalStateException("Reservation has expired");
            }
            
            // Get current inventory record
            var inventoryRecord = inventoryRepository.findBySkuAndStoreId(currentReservation.getSku(), currentReservation.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));
            
            // Controle otimista
            if (expectedVersion != null && !inventoryRecord.getVersion().equals(expectedVersion)) {
                throw new IllegalStateException(String.format("Version conflict. Expected: %d, Actual: %d", 
                        expectedVersion, inventoryRecord.getVersion()));
            }
            
            // Update reservation status
            currentReservation.setStatus(ReservationStatus.COMMITTED);
            reservationRepository.save(currentReservation);
            
            // Update inventory (commit reservation) - decrementa quantityAvailable e reserved
            InventoryRecord updatedRecord = InventoryRecord.builder()
                    .sku(currentReservation.getSku())
                    .storeId(currentReservation.getStoreId())
                    .quantityAvailable(inventoryRecord.getQuantityAvailable() - currentReservation.getQty())
                    .reserved(inventoryRecord.getReserved() - currentReservation.getQty())
                    .version(inventoryRecord.getVersion() + 1)
                    .updatedAt(Instant.now())
                    .build();
            
            InventoryRecord savedRecord = inventoryRepository.save(updatedRecord);
            eventBus.publish(new ReservationCommittedEvent(currentReservation));
            metricsService.incrementReservationsCommitted();
            
            // Publish cache invalidation event
            eventBus.publish(new CacheInvalidationEvent(currentReservation.getSku(), currentReservation.getStoreId(), "reservation_committed"));
            
            return savedRecord;
            });
        } finally {
            metricsService.recordCommitTimer(sample);
        }
    }

    public InventoryRecord release(String reservationId, Long expectedVersion) {
        log.info("Releasing reservation: {}, Expected Version: {}", reservationId, expectedVersion);
        
        Timer.Sample sample = metricsService.startReleaseTimer();
        Reservation reservation = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        
        try {
            return concurrencyService.executeWithLock(reservation.getSku(), reservation.getStoreId(), () -> {
            // Re-fetch reservation to ensure it's still valid
            Reservation currentReservation = reservationRepository.findByReservationId(reservationId)
                    .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
            
            if (currentReservation.getStatus() != ReservationStatus.CREATED) {
                throw new IllegalStateException("Reservation is not in CREATED status");
            }
            
            // Get current inventory record
            var inventoryRecord = inventoryRepository.findBySkuAndStoreId(currentReservation.getSku(), currentReservation.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));
            
            // Controle otimista
            if (expectedVersion != null && !inventoryRecord.getVersion().equals(expectedVersion)) {
                throw new IllegalStateException(String.format("Version conflict. Expected: %d, Actual: %d", 
                        expectedVersion, inventoryRecord.getVersion()));
            }
            
            // Update reservation status
            currentReservation.setStatus(ReservationStatus.RELEASED);
            reservationRepository.save(currentReservation);
            
            // Update inventory (release reservation) - decrementa somente reserved
            InventoryRecord updatedRecord = InventoryRecord.builder()
                    .sku(currentReservation.getSku())
                    .storeId(currentReservation.getStoreId())
                    .quantityAvailable(inventoryRecord.getQuantityAvailable())
                    .reserved(inventoryRecord.getReserved() - currentReservation.getQty())
                    .version(inventoryRecord.getVersion() + 1)
                    .updatedAt(Instant.now())
                    .build();
            
            InventoryRecord savedRecord = inventoryRepository.save(updatedRecord);
            eventBus.publish(new ReservationReleasedEvent(currentReservation));
            metricsService.incrementReservationsReleased();
            
            // Publish cache invalidation event
            eventBus.publish(new CacheInvalidationEvent(currentReservation.getSku(), currentReservation.getStoreId(), "reservation_released"));
            
            return savedRecord;
            });
        } finally {
            metricsService.recordReleaseTimer(sample);
        }
    }

    public GetItemResponse getItem(String sku) {
        log.info("Getting item details for SKU: {}", sku);
        
        // Try cache first
        Optional<GetItemResponse> cachedResponse = cacheService.getItemFromCache(sku);
        if (cachedResponse.isPresent()) {
            log.debug("Returning cached item response for SKU: {}", sku);
            return cachedResponse.get();
        }
        
        // Cache miss - fetch from repository
        Item item = itemRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Item with SKU " + sku + " not found"));
        
        List<InventoryRecord> availability = inventoryRepository.findBySku(sku);
        
        GetItemResponse response = new GetItemResponse();
        response.setItem(item);
        response.setAvailability(availability);
        
        // Write-through cache
        cacheService.putItemInCache(sku, response);
        
        return response;
    }

    public GetAvailabilityResponse getAvailability(List<String> skus, String storeId) {
        log.info("Getting availability for SKUs: {}, Store: {}", skus, storeId);
        
        // Try cache first
        Optional<GetAvailabilityResponse> cachedResponse = cacheService.getAvailabilityFromCache(skus, storeId);
        if (cachedResponse.isPresent()) {
            log.debug("Returning cached availability response for SKUs: {}", skus);
            return cachedResponse.get();
        }
        
        // Cache miss - fetch from repository
        String targetStoreId = storeId != null ? storeId : "GLOBAL";
        List<InventoryRecord> results = inventoryRepository.findBySkus(skus, targetStoreId);
        
        GetAvailabilityResponse response = new GetAvailabilityResponse();
        response.setResults(results);
        
        // Write-through cache
        cacheService.putAvailabilityInCache(skus, storeId, response);
        
        return response;
    }

    public SearchItemsResponse searchItems(String storeId, String q, Integer page, Integer pageSize) {
        log.info("Searching items with query: {}, storeId: {}, page: {}, pageSize: {}", q, storeId, page, pageSize);
        
        // Try cache first
        Optional<SearchItemsResponse> cachedResponse = cacheService.getSearchFromCache(q, storeId, page, pageSize);
        if (cachedResponse.isPresent()) {
            log.debug("Returning cached search response for query: {}", q);
            return cachedResponse.get();
        }
        
        // Cache miss - fetch from repository
        List<Item> items = itemRepository.findByQuery(q, storeId, page, pageSize);
        
        SearchItemsResponse response = new SearchItemsResponse();
        response.setItems(items);
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotal(items.size()); // Simplified - in real implementation would get total count
        
        // Write-through cache
        cacheService.putSearchInCache(q, storeId, page, pageSize, response);
        
        return response;
    }

    public void expireReservations() {
        log.info("Expiring reservations");
        
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(Instant.now());
        
        for (Reservation reservation : expiredReservations) {
            concurrencyService.executeWithLock(reservation.getSku(), reservation.getStoreId(), () -> {
                // Re-check if reservation is still expired
                if (reservation.getExpiresAt().isBefore(Instant.now()) && 
                    reservation.getStatus() == ReservationStatus.CREATED) {
                    
                    // Update reservation status
                    reservation.setStatus(ReservationStatus.EXPIRED);
                    reservationRepository.save(reservation);
                    
                    // Update inventory (release reserved quantity)
                    var inventoryRecord = inventoryRepository.findBySkuAndStoreId(reservation.getSku(), reservation.getStoreId())
                            .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));
                    
                    InventoryRecord updatedRecord = InventoryRecord.builder()
                            .sku(reservation.getSku())
                            .storeId(reservation.getStoreId())
                            .quantityAvailable(inventoryRecord.getQuantityAvailable())
                            .reserved(inventoryRecord.getReserved() - reservation.getQty())
                            .version(inventoryRecord.getVersion() + 1)
                            .updatedAt(Instant.now())
                            .build();
                    
                    inventoryRepository.save(updatedRecord);
                    eventBus.publish(new ReservationExpiredEvent(reservation));
                    metricsService.incrementReservationsExpired();
                    
                    // Publish cache invalidation event
                    eventBus.publish(new CacheInvalidationEvent(reservation.getSku(), reservation.getStoreId(), "reservation_expired"));
                }
            });
        }
    }

    public boolean isIdempotent(String idempotencyKey) {
        return idempotencyRepository.existsByKey(idempotencyKey);
    }

    public void saveIdempotentResult(String idempotencyKey, String result) {
        idempotencyRepository.save(idempotencyKey, result);
    }

    public String getIdempotentResult(String idempotencyKey) {
        return idempotencyRepository.findByKey(idempotencyKey).orElse(null);
    }

    private InventoryRecord createInitialRecord(String sku, String storeId) {
        return InventoryRecord.builder()
                .sku(sku)
                .storeId(storeId)
                .quantityAvailable(0L)
                .reserved(0L)
                .version(0L)
                .updatedAt(Instant.now())
                .build();
    }

    private void updateInventoryReserved(InventoryRecord inventoryRecord, Long qty) {
        InventoryRecord updatedRecord = InventoryRecord.builder()
                .sku(inventoryRecord.getSku())
                .storeId(inventoryRecord.getStoreId())
                .quantityAvailable(inventoryRecord.getQuantityAvailable())
                .reserved(inventoryRecord.getReserved() + qty)
                .version(inventoryRecord.getVersion() + 1)
                .updatedAt(Instant.now())
                .build();
        
        inventoryRepository.save(updatedRecord);
    }
}
