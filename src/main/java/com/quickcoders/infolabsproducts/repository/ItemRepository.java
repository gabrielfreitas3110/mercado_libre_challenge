package com.quickcoders.infolabsproducts.repository;

import com.quickcoders.infolabsproducts.domain.Item;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {
    Item save(Item item);
    Optional<Item> findBySku(String sku);
    List<Item> findAll();
    List<Item> findByQuery(String query, String storeId, int page, int pageSize);
    boolean existsBySku(String sku);
    void deleteBySku(String sku);
}
