package com.clinic.recp.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record InternalCreatePatientRequest(

        @Positive
        Long userId,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @Email
        @Size(max = 255)
        String email,

        @Pattern(regexp = "^\\+?[0-9 ]{7,20}$")
        String phone
) {
}