package com.quickcoders.infolabsproducts.web.dto;

import com.quickcoders.infolabsproducts.domain.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchItemsResponse {
    private List<Item> items;
    private Integer page;
    private Integer pageSize;
    private Integer total;
}
