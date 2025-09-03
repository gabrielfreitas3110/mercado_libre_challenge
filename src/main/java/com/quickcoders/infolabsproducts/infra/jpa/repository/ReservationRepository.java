package com.quickcoders.infolabsproducts.infra.jpa.repository;

import com.quickcoders.infolabsproducts.domain.enums.ReservationStatus;
import com.quickcoders.infolabsproducts.infra.jpa.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, String> {
    
    Optional<ReservationEntity> findByReservationId(String reservationId);
    
    List<ReservationEntity> findBySkuAndStoreId(String sku, String storeId);
    
    List<ReservationEntity> findByStatus(ReservationStatus status);
    
    @Query("SELECT r FROM ReservationEntity r WHERE r.expiresAt < :now AND r.status IN ('CREATED', 'RESERVED')")
    List<ReservationEntity> findExpiredReservations(@Param("now") Instant now);
    
    @Modifying
    @Query("UPDATE ReservationEntity r SET r.status = :status WHERE r.reservationId = :reservationId")
    int updateReservationStatus(@Param("reservationId") String reservationId, @Param("status") ReservationStatus status);
    
    @Modifying
    @Query("DELETE FROM ReservationEntity r WHERE r.expiresAt < :now AND r.status IN ('EXPIRED', 'RELEASED')")
    int deleteExpiredReservations(@Param("now") Instant now);
}
