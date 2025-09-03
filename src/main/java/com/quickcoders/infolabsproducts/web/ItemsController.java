package com.quickcoders.infolabsproducts.web;

import com.quickcoders.infolabsproducts.domain.Item;
import com.quickcoders.infolabsproducts.domain.InventoryRecord;
import com.quickcoders.infolabsproducts.domain.Adjustment;
import com.quickcoders.infolabsproducts.service.InventoryService;
import com.quickcoders.infolabsproducts.web.dto.CreateItemRequest;
import com.quickcoders.infolabsproducts.web.dto.AdjustRequest;
import com.quickcoders.infolabsproducts.web.dto.SearchItemsResponse;
import com.quickcoders.infolabsproducts.web.dto.GetItemResponse;
import com.quickcoders.infolabsproducts.web.dto.GetAvailabilityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemsController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<Item> createItem(@Valid @RequestBody CreateItemRequest request,
                                         @RequestHeader("Idempotency-Key") String idempotencyKey,
                                         HttpServletRequest httpRequest) {
        log.info("Creating item with SKU: {}, Idempotency-Key: {}", request.getSku(), idempotencyKey);
        
        Item item = Item.builder()
                .sku(request.getSku())
                .name(request.getName())
                .attributes(request.getAttributes())
                .build();
        
        Item createdItem = inventoryService.createItem(item);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", httpRequest.getRequestURL() + "/" + createdItem.getSku())
                .body(createdItem);
    }

    @PutMapping("/{sku}/adjust")
    public ResponseEntity<InventoryRecord> adjustInventory(@PathVariable String sku,
                                                          @Valid @RequestBody AdjustRequest request,
                                                          @RequestHeader("Idempotency-Key") String idempotencyKey) {
        log.info("Adjusting inventory for SKU: {}, Store: {}, Delta: {}, Idempotency-Key: {}", 
                sku, request.getStoreId(), request.getDelta(), idempotencyKey);
        
        Adjustment adjustment = Adjustment.builder()
                .sku(sku)
                .storeId(request.getStoreId())
                .delta(request.getDelta())
                .expectedVersion(request.getExpectedVersion())
                .build();
        
        InventoryRecord adjustedRecord = inventoryService.adjust(adjustment);
        
        return ResponseEntity.ok(adjustedRecord);
    }

    @GetMapping("/{sku}")
    public ResponseEntity<GetItemResponse> getItem(@PathVariable String sku) {
        log.info("Getting item details for SKU: {}", sku);
        
        GetItemResponse response = inventoryService.getItem(sku);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<SearchItemsResponse> searchItems(@RequestParam(required = false) String storeId,
                                                         @RequestParam(required = false) String q,
                                                         @RequestParam(defaultValue = "0") Integer page,
                                                         @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("Searching items with query: {}, storeId: {}, page: {}, pageSize: {}", q, storeId, page, pageSize);
        
        SearchItemsResponse response = inventoryService.searchItems(storeId, q, page, pageSize);
        
        return ResponseEntity.ok(response);
    }
}
