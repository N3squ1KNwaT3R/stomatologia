package com.clinic.recp.appointment.dto;

import com.clinic.recp.appointment.entity.AppointmentSource;
import com.clinic.recp.appointment.entity.AppointmentStatus;

import java.time.Instant;

public record InternalAppointmentResponse(
        Long id,
        Long patientId,
        Long doctorId,
        Long doctorUserId,
        Long serviceId,
        Instant startAt,
        Instant endAt,
        Instant blockedUntil,
        AppointmentStatus status,
        AppointmentSource source
) {
}