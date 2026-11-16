package com.clinic.recp.doctor.dto;

import jakarta.validation.constraints.Size;

public record UpdateDoctorRequest(

        @Size(max = 100)
        String specialization,

        @Size(max = 20)
        String room
) {
}