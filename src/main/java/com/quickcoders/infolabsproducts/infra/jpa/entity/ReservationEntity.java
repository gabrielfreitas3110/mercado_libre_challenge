package com.quickcoders.infolabsproducts.infra.jpa.entity;

import com.quickcoders.infolabsproducts.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationEntity {
    
    @Id
    @Column(name = "reservation_id", nullable = false, length = 100)
    private String reservationId;
    
    @Column(name = "sku", nullable = false, length = 100)
    private String sku;
    
    @Column(name = "store_id", nullable = false, length = 100)
    private String storeId;
    
    @Column(name = "qty", nullable = false)
    private Long qty;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;
    
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
