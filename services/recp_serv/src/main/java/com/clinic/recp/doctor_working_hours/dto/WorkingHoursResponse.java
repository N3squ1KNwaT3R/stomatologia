package com.clinic.recp.doctor_working_hours.dto;

import java.time.LocalTime;

public record WorkingHoursResponse(
        Long id,
        Long doctorId,
        Short weekday,
        LocalTime startTime,
        LocalTime endTime
) {
}