package com.clinic.recp.doctor.dto;

import com.clinic.recp.service_catalog.dto.ServiceResponse;

import java.util.List;

public record DoctorResponse(
        Long id,
        Long userId,
        String specialization,
        String room,
        List<ServiceResponse> services
) {
}
