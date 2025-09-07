package com.quickcoders.infolabsproducts.infra.jpa.repository;

import com.quickcoders.infolabsproducts.domain.Reservation;
import com.quickcoders.infolabsproducts.domain.enums.ReservationStatus;
import com.quickcoders.infolabsproducts.infra.jpa.entity.ReservationEntity;
import com.quickcoders.infolabsproducts.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("local")
@RequiredArgsConstructor
public class JpaReservationRepositoryAdapter implements ReservationRepository {
    
    private final ReservationRepository jpaReservationRepository;
    
    @Override
    public Reservation save(Reservation reservation) {
        ReservationEntity entity = ReservationEntity.builder()
                .reservationId(reservation.getReservationId())
                .sku(reservation.getSku())
                .storeId(reservation.getStoreId())
                .qty(reservation.getQty())
                .status(reservation.getStatus())
                .expiresAt(reservation.getExpiresAt())
                .build();
        
        ReservationEntity savedEntity = jpaReservationRepository.save(entity);
        
        return Reservation.builder()
                .reservationId(savedEntity.getReservationId())
                .sku(savedEntity.getSku())
                .storeId(savedEntity.getStoreId())
                .qty(savedEntity.getQty())
                .status(savedEntity.getStatus())
                .expiresAt(savedEntity.getExpiresAt())
                .createdAt(savedEntity.getCreatedAt())
                .build();
    }
    
    @Override
    public Optional<Reservation> findByReservationId(String reservationId) {
        return jpaReservationRepository.findByReservationId(reservationId)
                .map(this::toDomain);
    }
    
    @Override
    public List<Reservation> findBySkuAndStoreId(String sku, String storeId) {
        return jpaReservationRepository.findBySkuAndStoreId(sku, storeId).stream()
                .map(this::toDomain)
                .toList();
    }
    
    @Override
    public List<Reservation> findByStatus(ReservationStatus status) {
        return jpaReservationRepository.findByStatus(status).stream()
                .map(this::toDomain)
                .toList();
    }
    
    @Override
    public List<Reservation> findExpiredReservations(Instant now) {
        return jpaReservationRepository.findExpiredReservations(now).stream()
                .map(this::toDomain)
                .toList();
    }
    
    @Override
    public void deleteExpiredReservations(Instant now) {
        jpaReservationRepository.deleteExpiredReservations(now);
    }
    
    private Reservation toDomain(ReservationEntity entity) {
        return Reservation.builder()
                .reservationId(entity.getReservationId())
                .sku(entity.getSku())
                .storeId(entity.getStoreId())
                .qty(entity.getQty())
                .status(entity.getStatus())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

