package com.quickcoders.infolabsproducts.repository.impl;

import com.quickcoders.infolabsproducts.domain.Reservation;
import com.quickcoders.infolabsproducts.repository.ReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Profile("!file")
public class InMemoryReservationRepository implements ReservationRepository {
    
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getReservationId(), reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation> findByReservationId(String reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    @Override
    public List<Reservation> findBySkuAndStoreId(String sku, String storeId) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getSku().equals(sku) && 
                                     reservation.getStoreId().equals(storeId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findExpiredReservations(Instant before) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getExpiresAt() != null && 
                                     reservation.getExpiresAt().isBefore(before))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByReservationId(String reservationId) {
        return reservations.containsKey(reservationId);
    }

    @Override
    public void deleteByReservationId(String reservationId) {
        reservations.remove(reservationId);
    }

    @Override
    public void deleteExpiredReservations(Instant before) {
        List<String> expiredIds = findExpiredReservations(before).stream()
                .map(Reservation::getReservationId)
                .collect(Collectors.toList());
        
        expiredIds.forEach(reservations::remove);
    }
}
