package com.quickcoders.infolabsproducts.web;

import com.quickcoders.infolabsproducts.service.InventoryService;
import com.quickcoders.infolabsproducts.web.dto.GetAvailabilityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AvailabilityController {

    private final InventoryService inventoryService;

    @GetMapping("/availability")
    public ResponseEntity<GetAvailabilityResponse> getAvailability(@RequestParam List<String> skus,
                                                                   @RequestParam(required = false) String storeId) {
        log.info("Getting availability for SKUs: {}, Store: {}", skus, storeId);
        
        GetAvailabilityResponse response = inventoryService.getAvailability(skus, storeId);
        
        return ResponseEntity.ok(response);
    }
}
