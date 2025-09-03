package com.quickcoders.infolabsproducts.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adjustment Domain Tests")
class AdjustmentTest {

    @Test
    @DisplayName("Should create adjustment with all fields")
    void shouldCreateAdjustmentWithAllFields() {
        // Given
        String sku = "SKU-123";
        String storeId = "STORE-01";
        Long delta = 50L;
        Long expectedVersion = 5L;

        // When
        Adjustment adjustment = Adjustment.builder()
                .sku(sku)
                .storeId(storeId)
                .delta(delta)
                .expectedVersion(expectedVersion)
                .build();

        // Then
        assertNotNull(adjustment);
        assertEquals(sku, adjustment.getSku());
        assertEquals(storeId, adjustment.getStoreId());
        assertEquals(delta, adjustment.getDelta());
        assertEquals(expectedVersion, adjustment.getExpectedVersion());
    }

    @Test
    @DisplayName("Should create adjustment with default values")
    void shouldCreateAdjustmentWithDefaultValues() {
        // When
        Adjustment adjustment = new Adjustment();

        // Then
        assertNotNull(adjustment);
        assertNull(adjustment.getSku());
        assertNull(adjustment.getStoreId());
        assertNull(adjustment.getDelta());
        assertNull(adjustment.getExpectedVersion());
    }

    @Test
    @DisplayName("Should handle positive delta (stock increase)")
    void shouldHandlePositiveDelta() {
        // Given
        Adjustment adjustment = Adjustment.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .delta(100L)
                .expectedVersion(1L)
                .build();

        // Then
        assertTrue(adjustment.getDelta() > 0);
        assertEquals(100L, adjustment.getDelta());
    }

    @Test
    @DisplayName("Should handle negative delta (stock decrease)")
    void shouldHandleNegativeDelta() {
        // Given
        Adjustment adjustment = Adjustment.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .delta(-50L)
                .expectedVersion(1L)
                .build();

        // Then
        assertTrue(adjustment.getDelta() < 0);
        assertEquals(-50L, adjustment.getDelta());
    }

    @Test
    @DisplayName("Should handle zero delta")
    void shouldHandleZeroDelta() {
        // Given
        Adjustment adjustment = Adjustment.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .delta(0L)
                .expectedVersion(1L)
                .build();

        // Then
        assertEquals(0L, adjustment.getDelta());
    }

    @Test
    @DisplayName("Should handle null storeId (GLOBAL store)")
    void shouldHandleNullStoreId() {
        // Given
        Adjustment adjustment = Adjustment.builder()
                .sku("SKU-123")
                .storeId(null)
                .delta(50L)
                .expectedVersion(1L)
                .build();

        // Then
        assertNull(adjustment.getStoreId());
    }

    @Test
    @DisplayName("Should handle null expectedVersion (no optimistic locking)")
    void shouldHandleNullExpectedVersion() {
        // Given
        Adjustment adjustment = Adjustment.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .delta(50L)
                .expectedVersion(null)
                .build();

        // Then
        assertNull(adjustment.getExpectedVersion());
    }

    @Test
    @DisplayName("Should handle null values correctly")
    void shouldHandleNullValuesCorrectly() {
        // When
        Adjustment adjustment = Adjustment.builder()
                .sku(null)
                .storeId(null)
                .delta(null)
                .expectedVersion(null)
                .build();

        // Then
        assertNotNull(adjustment);
        assertNull(adjustment.getSku());
        assertNull(adjustment.getStoreId());
        assertNull(adjustment.getDelta());
        assertNull(adjustment.getExpectedVersion());
    }
}
