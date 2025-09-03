package com.quickcoders.infolabsproducts.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quickcoders.infolabsproducts.domain.Item;
import com.quickcoders.infolabsproducts.domain.InventoryRecord;
import com.quickcoders.infolabsproducts.web.dto.GetItemResponse;
import com.quickcoders.infolabsproducts.web.dto.GetAvailabilityResponse;
import com.quickcoders.infolabsproducts.web.dto.SearchItemsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CacheService {
    
    private final Cache<String, GetItemResponse> itemCache;
    private final Cache<String, GetAvailabilityResponse> availabilityCache;
    private final Cache<String, SearchItemsResponse> searchCache;
    
    public CacheService() {
        this.itemCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(8))
                .maximumSize(1000)
                .build();
                
        this.availabilityCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(8))
                .maximumSize(1000)
                .build();
                
        this.searchCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(8))
                .maximumSize(500)
                .build();
    }
    
    // Cache keys
    public static String getItemCacheKey(String sku) {
        return "stock:item:" + sku;
    }
    
    public static String getAvailabilityCacheKey(List<String> skus, String storeId) {
        String store = storeId != null ? storeId : "GLOBAL";
        return "stock:availability:" + String.join(",", skus) + ":" + store;
    }
    
    public static String getSearchCacheKey(String query, String storeId, int page, int pageSize) {
        String store = storeId != null ? storeId : "GLOBAL";
        return "stock:search:" + (query != null ? query : "") + ":" + store + ":" + page + ":" + pageSize;
    }
    
    public static String getInventoryCacheKey(String sku, String storeId) {
        String store = storeId != null ? storeId : "GLOBAL";
        return "stock:inventory:" + sku + ":" + store;
    }
    
    // Item cache operations
    public Optional<GetItemResponse> getItemFromCache(String sku) {
        String key = getItemCacheKey(sku);
        GetItemResponse cached = itemCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Cache hit for item: {}", sku);
            return Optional.of(cached);
        }
        log.debug("Cache miss for item: {}", sku);
        return Optional.empty();
    }
    
    public void putItemInCache(String sku, GetItemResponse response) {
        String key = getItemCacheKey(sku);
        itemCache.put(key, response);
        log.debug("Cached item response for SKU: {}", sku);
    }
    
    public void invalidateItemCache(String sku) {
        String key = getItemCacheKey(sku);
        itemCache.invalidate(key);
        log.debug("Invalidated item cache for SKU: {}", sku);
    }
    
    // Availability cache operations
    public Optional<GetAvailabilityResponse> getAvailabilityFromCache(List<String> skus, String storeId) {
        String key = getAvailabilityCacheKey(skus, storeId);
        GetAvailabilityResponse cached = availabilityCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Cache hit for availability: {}", key);
            return Optional.of(cached);
        }
        log.debug("Cache miss for availability: {}", key);
        return Optional.empty();
    }
    
    public void putAvailabilityInCache(List<String> skus, String storeId, GetAvailabilityResponse response) {
        String key = getAvailabilityCacheKey(skus, storeId);
        availabilityCache.put(key, response);
        log.debug("Cached availability response for: {}", key);
    }
    
    public void invalidateAvailabilityCache(String sku, String storeId) {
        // Invalidate all availability caches that might contain this SKU
        availabilityCache.asMap().keySet().stream()
                .filter(key -> key.contains(sku))
                .forEach(key -> {
                    availabilityCache.invalidate(key);
                    log.debug("Invalidated availability cache: {}", key);
                });
    }
    
    // Search cache operations
    public Optional<SearchItemsResponse> getSearchFromCache(String query, String storeId, int page, int pageSize) {
        String key = getSearchCacheKey(query, storeId, page, pageSize);
        SearchItemsResponse cached = searchCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Cache hit for search: {}", key);
            return Optional.of(cached);
        }
        log.debug("Cache miss for search: {}", key);
        return Optional.empty();
    }
    
    public void putSearchInCache(String query, String storeId, int page, int pageSize, SearchItemsResponse response) {
        String key = getSearchCacheKey(query, storeId, page, pageSize);
        searchCache.put(key, response);
        log.debug("Cached search response for: {}", key);
    }
    
    public void invalidateSearchCache(String sku) {
        // Invalidate all search caches that might contain this SKU
        searchCache.asMap().keySet().stream()
                .filter(key -> key.contains("stock:search:"))
                .forEach(key -> {
                    searchCache.invalidate(key);
                    log.debug("Invalidated search cache: {}", key);
                });
    }
    
    // Bulk invalidation for inventory changes
    public void invalidateAllCachesForSku(String sku, String storeId) {
        log.info("Invalidating all caches for SKU: {}, Store: {}", sku, storeId);
        
        // Invalidate item cache
        invalidateItemCache(sku);
        
        // Invalidate availability cache
        invalidateAvailabilityCache(sku, storeId);
        
        // Invalidate search cache
        invalidateSearchCache(sku);
    }
    
    // Cache statistics
    public void logCacheStats() {
        log.info("Cache Statistics - Item: hitRate={}, size={}, estimatedSize={}", 
                itemCache.stats().hitRate(), itemCache.estimatedSize(), itemCache.stats().hitCount());
        log.info("Cache Statistics - Availability: hitRate={}, size={}, estimatedSize={}", 
                availabilityCache.stats().hitRate(), availabilityCache.estimatedSize(), availabilityCache.stats().hitCount());
        log.info("Cache Statistics - Search: hitRate={}, size={}, estimatedSize={}", 
                searchCache.stats().hitRate(), searchCache.estimatedSize(), searchCache.stats().hitCount());
    }
}
