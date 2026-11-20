package com.clinic.recp.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record ReceptionCreateAppointmentRequest(

        @NotNull
        @Positive
        Long patientId,

        @NotNull
        @Positive
        Long doctorId,

        @NotNull
        @Positive
        Long serviceId,

        @NotNull
        OffsetDateTime startAt,

        @Positive
        Integer durationMin,

        @PositiveOrZero
        Integer bufferMin,

        @Size(max = 20)
        String room,

        String comment
) {
}