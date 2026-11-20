package com.clinic.recp.appointment.dto;

import com.clinic.recp.appointment.entity.AppointmentSource;
import com.clinic.recp.appointment.entity.AppointmentStatus;

import java.time.Instant;

public record AppointmentResponse(
        Long id,
        Long patientId,
        Long doctorId,
        Long serviceId,
        Instant startAt,
        Instant endAt,
        Instant blockedUntil,
        Integer durationMin,
        Integer bufferMin,
        AppointmentStatus status,
        AppointmentSource source,
        String room,
        String comment,
        Instant cancelledAt
) {
}