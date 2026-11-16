package com.clinic.recp.patient.controller;

import com.clinic.recp.patient.dto.CreatePatientRequest;
import com.clinic.recp.patient.dto.PatientResponse;
import com.clinic.recp.patient.dto.UpdatePatientRequest;
import com.clinic.recp.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    public PatientResponse create(
            @Valid @RequestBody CreatePatientRequest request
    ) {
        return patientService.create(request);
    }

    @GetMapping
    public List<PatientResponse> findAll(
            @RequestParam(required = false) String search
    ) {
        return patientService.findAll(search);
    }

    @GetMapping("/{id}")
    public PatientResponse findById(@PathVariable Long id) {
        return patientService.findById(id);
    }

    @PatchMapping("/{id}")
    public PatientResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePatientRequest request
    ) {
        return patientService.update(id, request);
    }

    @PostMapping("/{id}/verify")
    public PatientResponse verify(@PathVariable Long id) {
        return patientService.verify(id);
    }
}