package com.clinic.recp.patient.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PatientResponse(
        Long id,
        String firstName,
        String lastName,
        String pesel,
        LocalDate dateOfBirth,
        String phone,
        String email,
        String notes,
        Long userId,
        boolean isVerified,
        Instant createdAt
) {
}