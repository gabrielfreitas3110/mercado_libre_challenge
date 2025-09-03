package com.quickcoders.infolabsproducts.service;

import com.quickcoders.infolabsproducts.domain.Adjustment;
import com.quickcoders.infolabsproducts.domain.InventoryRecord;
import com.quickcoders.infolabsproducts.domain.Item;
import com.quickcoders.infolabsproducts.domain.Reservation;
import com.quickcoders.infolabsproducts.domain.events.*;
import com.quickcoders.infolabsproducts.repository.IdempotencyRepository;
import com.quickcoders.infolabsproducts.repository.InventoryRepository;
import com.quickcoders.infolabsproducts.repository.ItemRepository;
import com.quickcoders.infolabsproducts.repository.ReservationRepository;
import com.quickcoders.infolabsproducts.domain.enums.ReservationStatus;
import com.quickcoders.infolabsproducts.infra.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Inventory Service Tests")
class InventoryServiceTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private IdempotencyRepository idempotencyRepository;
    @Mock
    private EventBus eventBus;
    @Mock
    private ConcurrencyService concurrencyService;
    @Mock
    private CacheService cacheService;
    @Mock
    private MetricsService metricsService;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        inventoryService = new InventoryService(
                itemRepository,
                inventoryRepository,
                reservationRepository,
                idempotencyRepository,
                eventBus,
                concurrencyService,
                cacheService,
                metricsService
        );
    }

    @Test
    @DisplayName("Should create item successfully")
    void shouldCreateItemSuccessfully() {
        // Given
        Item item = Item.builder()
                .sku("SKU-123")
                .name("Test Item")
                .attributes(java.util.Map.of("color", "red"))
                .build();

        when(itemRepository.existsBySku("SKU-123")).thenReturn(false);
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        // When
        Item result = inventoryService.createItem(item);

        // Then
        assertNotNull(result);
        assertEquals("SKU-123", result.getSku());
        assertEquals("Test Item", result.getName());
        verify(itemRepository).save(item);
        verify(eventBus).publish(any(ItemCreatedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when creating duplicate item")
    void shouldThrowExceptionWhenCreatingDuplicateItem() {
        // Given
        Item item = Item.builder()
                .sku("SKU-123")
                .name("Test Item")
                .build();

        when(itemRepository.existsBySku("SKU-123")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.createItem(item);
        });

        assertEquals("Item with SKU SKU-123 already exists", exception.getMessage());
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    @DisplayName("Should adjust inventory successfully")
    void shouldAdjustInventorySuccessfully() {
        // Given
        Adjustment adjustment = Adjustment.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .delta(50L)
                .expectedVersion(1L)
                .build();

        InventoryRecord currentRecord = InventoryRecord.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .quantityAvailable(100L)
                .reserved(0L)
                .version(1L)
                .updatedAt(Instant.now())
                .build();

        when(concurrencyService.executeWithLock(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    // Execute the lambda
                    return ((java.util.function.Supplier<?>) invocation.getArgument(2)).get();
                });

        when(inventoryRepository.findBySkuAndStoreId("SKU-123", "STORE-01"))
                .thenReturn(Optional.of(currentRecord));
        when(inventoryRepository.save(any(InventoryRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        InventoryRecord result = inventoryService.adjust(adjustment);

        // Then
        assertNotNull(result);
        assertEquals(150L, result.getQuantityAvailable()); // 100 + 50
        assertEquals(2L, result.getVersion()); // 1 + 1
        verify(inventoryRepository).save(any(InventoryRecord.class));
        verify(eventBus).publish(any(InventoryAdjustedEvent.class));
        verify(eventBus).publish(any(CacheInvalidationEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when version conflict occurs")
    void shouldThrowExceptionWhenVersionConflictOccurs() {
        // Given
        Adjustment adjustment = Adjustment.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .delta(50L)
                .expectedVersion(1L)
                .build();

        InventoryRecord currentRecord = InventoryRecord.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .quantityAvailable(100L)
                .reserved(0L)
                .version(2L) // Different version
                .updatedAt(Instant.now())
                .build();

        when(concurrencyService.executeWithLock(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    return ((java.util.function.Supplier<?>) invocation.getArgument(2)).get();
                });

        when(inventoryRepository.findBySkuAndStoreId("SKU-123", "STORE-01"))
                .thenReturn(Optional.of(currentRecord));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            inventoryService.adjust(adjustment);
        });

        assertTrue(exception.getMessage().contains("Version conflict"));
        verify(metricsService).incrementVersionConflicts();
        verify(metricsService).incrementOptimisticLockingConflicts();
    }

    @Test
    @DisplayName("Should create reservation successfully")
    void shouldCreateReservationSuccessfully() {
        // Given
        String sku = "SKU-123";
        String storeId = "STORE-01";
        Long qty = 5L;
        Long ttlSeconds = 900L;
        Long expectedVersion = 1L;

        InventoryRecord inventoryRecord = InventoryRecord.builder()
                .sku(sku)
                .storeId(storeId)
                .quantityAvailable(100L)
                .reserved(0L)
                .version(1L)
                .updatedAt(Instant.now())
                .build();

        when(concurrencyService.executeWithLock(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    return ((java.util.function.Supplier<?>) invocation.getArgument(2)).get();
                });

        when(inventoryRepository.findBySkuAndStoreId(sku, storeId))
                .thenReturn(Optional.of(inventoryRecord));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.save(any(InventoryRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Reservation result = inventoryService.reserve(sku, storeId, qty, ttlSeconds, expectedVersion);

        // Then
        assertNotNull(result);
        assertEquals(sku, result.getSku());
        assertEquals(storeId, result.getStoreId());
        assertEquals(qty, result.getQty());
        assertEquals(ReservationStatus.CREATED, result.getStatus());
        assertNotNull(result.getReservationId());
        assertNotNull(result.getExpiresAt());
        assertNotNull(result.getCreatedAt());

        verify(reservationRepository).save(any(Reservation.class));
        verify(inventoryRepository).save(any(InventoryRecord.class));
        verify(eventBus).publish(any(ReservationCreatedEvent.class));
        verify(eventBus).publish(any(CacheInvalidationEvent.class));
        verify(metricsService).incrementReservationsCreated();
    }

    @Test
    @DisplayName("Should throw exception when insufficient inventory for reservation")
    void shouldThrowExceptionWhenInsufficientInventoryForReservation() {
        // Given
        String sku = "SKU-123";
        String storeId = "STORE-01";
        Long qty = 150L; // More than available
        Long ttlSeconds = 900L;
        Long expectedVersion = 1L;

        InventoryRecord inventoryRecord = InventoryRecord.builder()
                .sku(sku)
                .storeId(storeId)
                .quantityAvailable(100L)
                .reserved(0L)
                .version(1L)
                .updatedAt(Instant.now())
                .build();

        when(concurrencyService.executeWithLock(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    return ((java.util.function.Supplier<?>) invocation.getArgument(2)).get();
                });

        when(inventoryRepository.findBySkuAndStoreId(sku, storeId))
                .thenReturn(Optional.of(inventoryRecord));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            inventoryService.reserve(sku, storeId, qty, ttlSeconds, expectedVersion);
        });

        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    @DisplayName("Should commit reservation successfully")
    void shouldCommitReservationSuccessfully() {
        // Given
        String reservationId = "RES-123";
        Long expectedVersion = 1L;

        Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .sku("SKU-123")
                .storeId("STORE-01")
                .qty(5L)
                .status(ReservationStatus.CREATED)
                .expiresAt(Instant.now().plusSeconds(900))
                .createdAt(Instant.now())
                .build();

        InventoryRecord inventoryRecord = InventoryRecord.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .quantityAvailable(100L)
                .reserved(5L)
                .version(1L)
                .updatedAt(Instant.now())
                .build();

        when(concurrencyService.executeWithLock(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    return ((java.util.function.Supplier<?>) invocation.getArgument(2)).get();
                });

        when(reservationRepository.findByReservationId(reservationId))
                .thenReturn(Optional.of(reservation));
        when(inventoryRepository.findBySkuAndStoreId("SKU-123", "STORE-01"))
                .thenReturn(Optional.of(inventoryRecord));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.save(any(InventoryRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        InventoryRecord result = inventoryService.commit(reservationId, expectedVersion);

        // Then
        assertNotNull(result);
        assertEquals(95L, result.getQuantityAvailable()); // 100 - 5
        assertEquals(0L, result.getReserved()); // 5 - 5
        assertEquals(2L, result.getVersion()); // 1 + 1

        verify(reservationRepository).save(any(Reservation.class));
        verify(inventoryRepository).save(any(InventoryRecord.class));
        verify(eventBus).publish(any(ReservationCommittedEvent.class));
        verify(eventBus).publish(any(CacheInvalidationEvent.class));
        verify(metricsService).incrementReservationsCommitted();
    }

    @Test
    @DisplayName("Should release reservation successfully")
    void shouldReleaseReservationSuccessfully() {
        // Given
        String reservationId = "RES-123";
        Long expectedVersion = 1L;

        Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .sku("SKU-123")
                .storeId("STORE-01")
                .qty(5L)
                .status(ReservationStatus.CREATED)
                .expiresAt(Instant.now().plusSeconds(900))
                .createdAt(Instant.now())
                .build();

        InventoryRecord inventoryRecord = InventoryRecord.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .quantityAvailable(100L)
                .reserved(5L)
                .version(1L)
                .updatedAt(Instant.now())
                .build();

        when(concurrencyService.executeWithLock(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    return ((java.util.function.Supplier<?>) invocation.getArgument(2)).get();
                });

        when(reservationRepository.findByReservationId(reservationId))
                .thenReturn(Optional.of(reservation));
        when(inventoryRepository.findBySkuAndStoreId("SKU-123", "STORE-01"))
                .thenReturn(Optional.of(inventoryRecord));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.save(any(InventoryRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        InventoryRecord result = inventoryService.release(reservationId, expectedVersion);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getQuantityAvailable()); // Unchanged
        assertEquals(0L, result.getReserved()); // 5 - 5
        assertEquals(2L, result.getVersion()); // 1 + 1

        verify(reservationRepository).save(any(Reservation.class));
        verify(inventoryRepository).save(any(InventoryRecord.class));
        verify(eventBus).publish(any(ReservationReleasedEvent.class));
        verify(eventBus).publish(any(CacheInvalidationEvent.class));
        verify(metricsService).incrementReservationsReleased();
    }

    @Test
    @DisplayName("Should expire reservations successfully")
    void shouldExpireReservationsSuccessfully() {
        // Given
        Reservation expiredReservation = Reservation.builder()
                .reservationId("RES-123")
                .sku("SKU-123")
                .storeId("STORE-01")
                .qty(5L)
                .status(ReservationStatus.CREATED)
                .expiresAt(Instant.now().minusSeconds(100)) // Expired
                .createdAt(Instant.now().minusSeconds(1000))
                .build();

        InventoryRecord inventoryRecord = InventoryRecord.builder()
                .sku("SKU-123")
                .storeId("STORE-01")
                .quantityAvailable(100L)
                .reserved(5L)
                .version(1L)
                .updatedAt(Instant.now())
                .build();

        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(concurrencyService).executeWithLock(anyString(), anyString(), any(Runnable.class));

        when(reservationRepository.findExpiredReservations(any(Instant.class)))
                .thenReturn(List.of(expiredReservation));
        when(reservationRepository.findByReservationId("RES-123"))
                .thenReturn(Optional.of(expiredReservation));
        when(inventoryRepository.findBySkuAndStoreId("SKU-123", "STORE-01"))
                .thenReturn(Optional.of(inventoryRecord));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.save(any(InventoryRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        inventoryService.expireReservations();

        // Then
        verify(reservationRepository).findExpiredReservations(any(Instant.class));
        verify(reservationRepository).save(any(Reservation.class));
        verify(inventoryRepository).save(any(InventoryRecord.class));
        verify(eventBus).publish(any(ReservationExpiredEvent.class));
        verify(eventBus).publish(any(CacheInvalidationEvent.class));
        verify(metricsService).incrementReservationsExpired();
    }

    @Test
    @DisplayName("Should handle idempotency operations")
    void shouldHandleIdempotencyOperations() {
        // Given
        String idempotencyKey = "test-key-123";
        String result = "{\"status\":\"success\"}";

        when(idempotencyRepository.existsByKey(idempotencyKey)).thenReturn(true);
        when(idempotencyRepository.findByKey(idempotencyKey)).thenReturn(Optional.of(result));

        // When
        boolean isIdempotent = inventoryService.isIdempotent(idempotencyKey);
        String idempotentResult = inventoryService.getIdempotentResult(idempotencyKey);

        // Then
        assertTrue(isIdempotent);
        assertEquals(result, idempotentResult);

        // Test saving idempotent result
        inventoryService.saveIdempotentResult(idempotencyKey, result);
        verify(idempotencyRepository).save(idempotencyKey, result);
    }
}
