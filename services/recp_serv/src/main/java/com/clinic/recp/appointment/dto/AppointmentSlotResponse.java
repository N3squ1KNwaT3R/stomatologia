package com.clinic.recp.appointment.dto;

import java.time.OffsetDateTime;

public record AppointmentSlotResponse(
        OffsetDateTime startAt,
        OffsetDateTime endAt
) {
}