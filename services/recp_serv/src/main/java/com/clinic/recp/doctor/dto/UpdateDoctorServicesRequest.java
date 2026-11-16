package com.clinic.recp.doctor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record UpdateDoctorServicesRequest(

        @NotNull
        List<@NotNull @Positive Long> serviceIds
) {
}
