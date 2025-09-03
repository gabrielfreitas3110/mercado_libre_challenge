package com.quickcoders.infolabsproducts.service;

import com.quickcoders.infolabsproducts.domain.InventoryRecord;
import com.quickcoders.infolabsproducts.domain.Reservation;
import com.quickcoders.infolabsproducts.domain.enums.ReservationStatus;
import com.quickcoders.infolabsproducts.repository.InventoryRepository;
import com.quickcoders.infolabsproducts.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {
    
    private final ReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;

    public Reservation createReservation(String sku, String storeId, Long qty, Integer ttlSeconds) {
        log.info("Creating reservation for SKU: {}, Store: {}, Qty: {}, TTL: {}s", 
                sku, storeId, qty, ttlSeconds);
        
        // Check inventory availability
        var inventoryRecord = inventoryRepository.findBySkuAndStoreId(sku, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for SKU: " + sku));
        
        if (inventoryRecord.getQuantityAvailable() < qty) {
            throw new IllegalArgumentException("Insufficient inventory. Available: " + 
                    inventoryRecord.getQuantityAvailable() + ", Requested: " + qty);
        }
        
        // Create reservation
        Reservation reservation = Reservation.builder()
                .reservationId(UUID.randomUUID().toString())
                .sku(sku)
                .storeId(storeId)
                .qty(qty)
                .status(ReservationStatus.CREATED)
                .expiresAt(Instant.now().plusSeconds(ttlSeconds))
                .createdAt(Instant.now())
                .build();
        
        // Update inventory (reserve quantity)
        updateInventoryReserved(inventoryRecord, qty);
        
        return reservationRepository.save(reservation);
    }

    public InventoryRecord commitReservation(String sku, String reservationId) {
        log.info("Committing reservation: {} for SKU: {}", reservationId, sku);
        
        Reservation reservation = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        
        if (!reservation.getSku().equals(sku)) {
            throw new IllegalArgumentException("Reservation SKU mismatch");
        }
        
        if (reservation.getStatus() != ReservationStatus.CREATED) {
            throw new IllegalStateException("Reservation is not in CREATED status");
        }
        
        if (reservation.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Reservation has expired");
        }
        
        // Update reservation status
        reservation.setStatus(ReservationStatus.COMMITTED);
        reservationRepository.save(reservation);
        
        // Update inventory (commit reservation)
        var inventoryRecord = inventoryRepository.findBySkuAndStoreId(sku, reservation.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));
        
        InventoryRecord updatedRecord = InventoryRecord.builder()
                .sku(sku)
                .storeId(reservation.getStoreId())
                .quantityAvailable(inventoryRecord.getQuantityAvailable() - reservation.getQty())
                .reserved(inventoryRecord.getReserved() - reservation.getQty())
                .version(inventoryRecord.getVersion() + 1)
                .updatedAt(Instant.now())
                .build();
        
        return inventoryRepository.save(updatedRecord);
    }

    public InventoryRecord releaseReservation(String sku, String reservationId) {
        log.info("Releasing reservation: {} for SKU: {}", reservationId, sku);
        
        Reservation reservation = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        
        if (!reservation.getSku().equals(sku)) {
            throw new IllegalArgumentException("Reservation SKU mismatch");
        }
        
        if (reservation.getStatus() != ReservationStatus.CREATED) {
            throw new IllegalStateException("Reservation is not in CREATED status");
        }
        
        // Update reservation status
        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);
        
        // Update inventory (release reservation)
        var inventoryRecord = inventoryRepository.findBySkuAndStoreId(sku, reservation.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));
        
        InventoryRecord updatedRecord = InventoryRecord.builder()
                .sku(sku)
                .storeId(reservation.getStoreId())
                .quantityAvailable(inventoryRecord.getQuantityAvailable())
                .reserved(inventoryRecord.getReserved() - reservation.getQty())
                .version(inventoryRecord.getVersion() + 1)
                .updatedAt(Instant.now())
                .build();
        
        return inventoryRepository.save(updatedRecord);
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
