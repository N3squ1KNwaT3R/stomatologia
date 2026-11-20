package com.clinic.recp.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

public record PatientCreateAppointmentRequest(

        @NotNull
        @Positive
        Long doctorId,

        @NotNull
        @Positive
        Long serviceId,

        @NotNull
        OffsetDateTime startAt,

        String comment
) {
}