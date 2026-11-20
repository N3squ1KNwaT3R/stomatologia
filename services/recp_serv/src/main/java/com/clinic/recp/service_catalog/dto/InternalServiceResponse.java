package com.clinic.recp.service_catalog.dto;

import java.math.BigDecimal;

public record InternalServiceResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer durationMin,
        Integer bufferMin,
        Boolean isActive
) {
}