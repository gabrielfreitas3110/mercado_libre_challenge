package com.quickcoders.infolabsproducts.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InventoryRecord Domain Tests")
class InventoryRecordTest {

    @Test
    @DisplayName("Should create inventory record with all fields")
    void shouldCreateInventoryRecordWithAllFields() {
        // Given
        String sku = "SKU-123";
        String storeId = "STORE-01";
        Long quantityAvailable = 100L;
        Long reserved = 10L;
        Long version = 1L;
        Instant updatedAt = Instant.now();

        // When
        InventoryRecord record = InventoryRecord.builder()
                .sku(sku)
                .storeId(storeId)
                .quantityAvailable(quantityAvailable)
                .reserved(reserved)
                .version(version)
                .updatedAt(updatedAt)
                .build();

        // Then
        assertNotNull(record);
        assertEquals(sku, record.getSku());
        assertEquals(storeId, record.getStoreId());
        assertEquals(quantityAvailable, record.getQuantityAvailable());
        assertEquals(reserved, record.getReserved());
        assertEquals(version, record.getVersion());
        assertEquals(updatedAt, record.getUpdatedAt());
    }

    @Test
    @DisplayName("Should create inventory record with default values")
    void shouldCreateInventoryRecordWithDefaultValues() {
        // When
        InventoryRecord record = new InventoryRecord();

        // Then
        assertNotNull(record);
        assertNull(record.getSku());
        assertNull(record.getStoreId());
        assertNull(record.getQuantityAvailable());
        assertNull(record.getReserved());
        assertNull(record.getVersion());
        assertNull(record.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle null values correctly")
    void shouldHandleNullValuesCorrectly() {
        // When
        InventoryRecord record = InventoryRecord.builder()
                .sku(null)
                .storeId(null)
                .quantityAvailable(null)
                .reserved(null)
                .version(null)
                .updatedAt(null)
                .build();

        // Then
        assertNotNull(record);
        assertNull(record.getSku());
        assertNull(record.getStoreId());
        assertNull(record.getQuantityAvailable());
        assertNull(record.getReserved());
        assertNull(record.getVersion());
        assertNull(record.getUpdatedAt());
    }

    @Test
    @DisplayName("Should calculate available quantity correctly")
    void shouldCalculateAvailableQuantityCorrectly() {
        // Given
        InventoryRecord record = InventoryRecord.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .quantityAvailable(100L)
                .reserved(20L)
                .version(1L)
                .updatedAt(Instant.now())
                .build();

        // When & Then
        assertEquals(100L, record.getQuantityAvailable());
        assertEquals(20L, record.getReserved());
        // Available for new reservations = quantityAvailable - reserved
        assertEquals(80L, record.getQuantityAvailable() - record.getReserved());
    }

    @Test
    @DisplayName("Should handle zero values correctly")
    void shouldHandleZeroValuesCorrectly() {
        // Given
        InventoryRecord record = InventoryRecord.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .quantityAvailable(0L)
                .reserved(0L)
                .version(0L)
                .updatedAt(Instant.now())
                .build();

        // Then
        assertEquals(0L, record.getQuantityAvailable());
        assertEquals(0L, record.getReserved());
        assertEquals(0L, record.getVersion());
    }
}
