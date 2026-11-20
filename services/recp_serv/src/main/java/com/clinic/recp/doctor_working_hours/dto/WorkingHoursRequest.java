package com.clinic.recp.doctor_working_hours.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record WorkingHoursRequest(

        @NotNull
        @Min(0)
        @Max(6)
        Short weekday,

        @NotNull
        LocalTime startTime,

        @NotNull
        LocalTime endTime

) {
}