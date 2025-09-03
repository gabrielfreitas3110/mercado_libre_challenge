package com.quickcoders.infolabsproducts.domain;

import com.quickcoders.infolabsproducts.domain.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    private String reservationId;
    private String sku;
    private String storeId;
    private Long qty;
    private ReservationStatus status;
    private Instant expiresAt;
    private Instant createdAt;
}
