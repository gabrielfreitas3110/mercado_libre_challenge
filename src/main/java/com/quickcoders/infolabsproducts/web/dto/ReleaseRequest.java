package com.quickcoders.infolabsproducts.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseRequest {
    @NotNull(message = "Reservation ID is required")
    @NotBlank(message = "Reservation ID cannot be empty")
    private String reservationId;
    
    private Long expectedVersion;
}
