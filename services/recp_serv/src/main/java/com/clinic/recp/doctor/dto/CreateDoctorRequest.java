package com.clinic.recp.doctor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateDoctorRequest(

        @NotNull
        @Positive
        Long userId,

        @Size(max = 100)
        String specialization,

        @Size(max = 20)
        String room
) {
}
