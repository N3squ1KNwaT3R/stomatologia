package com.clinic.recp.patient.controller;

import com.clinic.recp.patient.dto.InternalCreatePatientRequest;
import com.clinic.recp.patient.dto.PatientResponse;
import com.clinic.recp.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/patients")
@RequiredArgsConstructor
public class InternalPatientController {

    private final PatientService patientService;

    @PostMapping
    public PatientResponse create(
            @Valid @RequestBody InternalCreatePatientRequest request
    ) {
        return patientService.createForUser(request);
    }

    @GetMapping("/by-user/{user_id}")
    public PatientResponse findByUserId(
            @PathVariable("user_id") Long userId
    ) {
        return patientService.findByUserId(userId);
    }
}