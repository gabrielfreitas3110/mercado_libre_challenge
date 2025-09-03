package com.quickcoders.infolabsproducts.repository;

import com.quickcoders.infolabsproducts.domain.Reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findByReservationId(String reservationId);
    List<Reservation> findBySkuAndStoreId(String sku, String storeId);
    List<Reservation> findExpiredReservations(Instant before);
    boolean existsByReservationId(String reservationId);
    void deleteByReservationId(String reservationId);
    void deleteExpiredReservations(Instant before);
}
