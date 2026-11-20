package com.clinic.recp.service_catalog.dto;

import java.math.BigDecimal;

public record ServiceResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer durationMin,
        Integer bufferMin,
        Boolean isActive
) {
}