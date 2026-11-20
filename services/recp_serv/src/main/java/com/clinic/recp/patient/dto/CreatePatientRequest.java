package com.clinic.recp.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePatientRequest(

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @Pattern(regexp = "^\\d{11}$")
        String pesel,

        @Past
        LocalDate dateOfBirth,

        @Pattern(regexp = "^\\+?[0-9 ]{7,20}$")
        String phone,

        @Email
        @Size(max = 255)
        String email,

        String notes
) {
}