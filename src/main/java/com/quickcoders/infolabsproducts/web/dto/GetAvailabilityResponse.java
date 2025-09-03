package com.quickcoders.infolabsproducts.web.dto;

import com.quickcoders.infolabsproducts.domain.InventoryRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAvailabilityResponse {
    private List<InventoryRecord> results;
}
