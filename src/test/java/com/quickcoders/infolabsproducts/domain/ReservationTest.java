package com.quickcoders.infolabsproducts.domain;

import com.quickcoders.infolabsproducts.domain.enums.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Reservation Domain Tests")
class ReservationTest {

    @Test
    @DisplayName("Should create reservation with all fields")
    void shouldCreateReservationWithAllFields() {
        // Given
        String reservationId = "RES-123";
        String sku = "SKU-123";
        String storeId = "STORE-01";
        Long qty = 5L;
        ReservationStatus status = ReservationStatus.CREATED;
        Instant expiresAt = Instant.now().plusSeconds(900);
        Instant createdAt = Instant.now();

        // When
        Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .sku(sku)
                .storeId(storeId)
                .qty(qty)
                .status(status)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .build();

        // Then
        assertNotNull(reservation);
        assertEquals(reservationId, reservation.getReservationId());
        assertEquals(sku, reservation.getSku());
        assertEquals(storeId, reservation.getStoreId());
        assertEquals(qty, reservation.getQty());
        assertEquals(status, reservation.getStatus());
        assertEquals(expiresAt, reservation.getExpiresAt());
        assertEquals(createdAt, reservation.getCreatedAt());
    }

    @Test
    @DisplayName("Should create reservation with default values")
    void shouldCreateReservationWithDefaultValues() {
        // When
        Reservation reservation = new Reservation();

        // Then
        assertNotNull(reservation);
        assertNull(reservation.getReservationId());
        assertNull(reservation.getSku());
        assertNull(reservation.getStoreId());
        assertNull(reservation.getQty());
        assertNull(reservation.getStatus());
        assertNull(reservation.getExpiresAt());
        assertNull(reservation.getCreatedAt());
    }

    @Test
    @DisplayName("Should check if reservation is expired")
    void shouldCheckIfReservationIsExpired() {
        // Given
        Instant pastTime = Instant.now().minusSeconds(100);
        Instant futureTime = Instant.now().plusSeconds(100);

        Reservation expiredReservation = Reservation.builder()
                .reservationId("RES-1")
                .sku("SKU-123")
                .storeId("STORE-01")
                .qty(5L)
                .status(ReservationStatus.CREATED)
                .expiresAt(pastTime)
                .createdAt(Instant.now().minusSeconds(200))
                .build();

        Reservation activeReservation = Reservation.builder()
                .reservationId("RES-2")
                .sku("SKU-123")
                .storeId("STORE-01")
                .qty(5L)
                .status(ReservationStatus.CREATED)
                .expiresAt(futureTime)
                .createdAt(Instant.now())
                .build();

        // When & Then
        assertTrue(expiredReservation.getExpiresAt().isBefore(Instant.now()));
        assertFalse(activeReservation.getExpiresAt().isBefore(Instant.now()));
    }

    @Test
    @DisplayName("Should handle different reservation statuses")
    void shouldHandleDifferentReservationStatuses() {
        // Given
        String reservationId = "RES-123";
        String sku = "SKU-123";
        String storeId = "STORE-01";
        Long qty = 5L;
        Instant expiresAt = Instant.now().plusSeconds(900);
        Instant createdAt = Instant.now();

        // When
        Reservation createdReservation = Reservation.builder()
                .reservationId(reservationId)
                .sku(sku)
                .storeId(storeId)
                .qty(qty)
                .status(ReservationStatus.CREATED)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .build();

        Reservation committedReservation = Reservation.builder()
                .reservationId(reservationId)
                .sku(sku)
                .storeId(storeId)
                .qty(qty)
                .status(ReservationStatus.COMMITTED)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .build();

        Reservation releasedReservation = Reservation.builder()
                .reservationId(reservationId)
                .sku(sku)
                .storeId(storeId)
                .qty(qty)
                .status(ReservationStatus.RELEASED)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .build();

        Reservation expiredReservation = Reservation.builder()
                .reservationId(reservationId)
                .sku(sku)
                .storeId(storeId)
                .qty(qty)
                .status(ReservationStatus.EXPIRED)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .build();

        // Then
        assertEquals(ReservationStatus.CREATED, createdReservation.getStatus());
        assertEquals(ReservationStatus.COMMITTED, committedReservation.getStatus());
        assertEquals(ReservationStatus.RELEASED, releasedReservation.getStatus());
        assertEquals(ReservationStatus.EXPIRED, expiredReservation.getStatus());
    }

    @Test
    @DisplayName("Should handle zero quantity")
    void shouldHandleZeroQuantity() {
        // Given
        Reservation reservation = Reservation.builder()
                .reservationId("RES-123")
                .sku("SKU-123")
                .storeId("STORE-01")
                .qty(0L)
                .status(ReservationStatus.CREATED)
                .expiresAt(Instant.now().plusSeconds(900))
                .createdAt(Instant.now())
                .build();

        // Then
        assertEquals(0L, reservation.getQty());
    }

    @Test
    @DisplayName("Should handle null values correctly")
    void shouldHandleNullValuesCorrectly() {
        // When
        Reservation reservation = Reservation.builder()
                .reservationId(null)
                .sku(null)
                .storeId(null)
                .qty(null)
                .status(null)
                .expiresAt(null)
                .createdAt(null)
                .build();

        // Then
        assertNotNull(reservation);
        assertNull(reservation.getReservationId());
        assertNull(reservation.getSku());
        assertNull(reservation.getStoreId());
        assertNull(reservation.getQty());
        assertNull(reservation.getStatus());
        assertNull(reservation.getExpiresAt());
        assertNull(reservation.getCreatedAt());
    }
}
