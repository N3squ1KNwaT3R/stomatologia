package com.clinic.recp.doctor_working_hours.controller;

import com.clinic.recp.doctor_working_hours.dto.WorkingHoursRequest;
import com.clinic.recp.doctor_working_hours.dto.WorkingHoursResponse;
import com.clinic.recp.doctor_working_hours.service.DoctorWorkingHoursService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctors/{doctorId}/working-hours")
public class DoctorWorkingHoursController {

    private final DoctorWorkingHoursService workingHoursService;

    public DoctorWorkingHoursController(
            DoctorWorkingHoursService workingHoursService
    ) {
        this.workingHoursService = workingHoursService;
    }

    @GetMapping
    public List<WorkingHoursResponse> getWorkingHours(
            @PathVariable Long doctorId
    ) {
        return workingHoursService.getWorkingHours(doctorId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public List<WorkingHoursResponse> replaceWorkingHours(
            @PathVariable Long doctorId,
            @Valid @RequestBody List<@Valid WorkingHoursRequest> requests
    ) {
        return workingHoursService.replaceWorkingHours(doctorId, requests);
    }
}