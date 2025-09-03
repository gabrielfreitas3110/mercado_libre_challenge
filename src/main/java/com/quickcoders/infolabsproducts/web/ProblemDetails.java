package com.quickcoders.infolabsproducts.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetails {
    private URI type;
    private String title;
    private Integer status;
    private String detail;
    private URI instance;
    private Map<String, String[]> errors;
    
    // Campos específicos para conflitos de versão e estoque
    private String sku;
    private String storeId;
    private Long expectedVersion;
    private Long actualVersion;
    private String reason;
}
