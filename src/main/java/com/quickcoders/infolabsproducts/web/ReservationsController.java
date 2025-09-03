package com.quickcoders.infolabsproducts.web;

import com.quickcoders.infolabsproducts.domain.Reservation;
import com.quickcoders.infolabsproducts.domain.InventoryRecord;
import com.quickcoders.infolabsproducts.service.InventoryService;
import com.quickcoders.infolabsproducts.web.dto.ReserveRequest;
import com.quickcoders.infolabsproducts.web.dto.CommitRequest;
import com.quickcoders.infolabsproducts.web.dto.ReleaseRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ReservationsController {

    private final InventoryService inventoryService;

    @PostMapping("/{sku}/reserve")
    public ResponseEntity<Reservation> createReservation(@PathVariable String sku,
                                                        @Valid @RequestBody ReserveRequest request,
                                                        @RequestHeader("Idempotency-Key") String idempotencyKey) {
        log.info("Creating reservation for SKU: {}, Qty: {}, TTL: {}s, Expected Version: {}, Idempotency-Key: {}", 
                sku, request.getQty(), request.getTtlSeconds(), request.getExpectedVersion(), idempotencyKey);
        
        Reservation reservation = inventoryService.reserve(sku, request.getStoreId(), request.getQty(), 
                                                          request.getTtlSeconds(), request.getExpectedVersion());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @PostMapping("/{sku}/commit")
    public ResponseEntity<InventoryRecord> commitReservation(@PathVariable String sku,
                                                            @Valid @RequestBody CommitRequest request,
                                                            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        log.info("Committing reservation: {} for SKU: {}, Expected Version: {}, Idempotency-Key: {}", 
                request.getReservationId(), sku, request.getExpectedVersion(), idempotencyKey);
        
        InventoryRecord updatedRecord = inventoryService.commit(request.getReservationId(), request.getExpectedVersion());
        
        return ResponseEntity.ok(updatedRecord);
    }

    @PostMapping("/{sku}/release")
    public ResponseEntity<InventoryRecord> releaseReservation(@PathVariable String sku,
                                                             @Valid @RequestBody ReleaseRequest request,
                                                             @RequestHeader("Idempotency-Key") String idempotencyKey) {
        log.info("Releasing reservation: {} for SKU: {}, Expected Version: {}, Idempotency-Key: {}", 
                request.getReservationId(), sku, request.getExpectedVersion(), idempotencyKey);
        
        InventoryRecord updatedRecord = inventoryService.release(request.getReservationId(), request.getExpectedVersion());
        
        return ResponseEntity.ok(updatedRecord);
    }
}
