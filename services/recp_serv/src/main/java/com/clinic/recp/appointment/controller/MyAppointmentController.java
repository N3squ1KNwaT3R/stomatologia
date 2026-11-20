package com.clinic.recp.appointment.controller;

import com.clinic.recp.appointment.dto.AppointmentResponse;
import com.clinic.recp.appointment.dto.PatientCreateAppointmentRequest;
import com.clinic.recp.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/me/appointments")
@PreAuthorize("hasRole('PATIENT')")
public class MyAppointmentController {

    private final AppointmentService appointmentService;

    public MyAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createAppointment(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PatientCreateAppointmentRequest request
    ) {
        return appointmentService.createForPatient(userId, request);
    }

    @GetMapping
    public List<AppointmentResponse> getMyAppointments(
            @AuthenticationPrincipal Long userId
    ) {
        return appointmentService.getMyAppointments(userId);
    }

    @PostMapping("/{appointmentId}/cancel")
    public AppointmentResponse cancelAppointment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long appointmentId
    ) {
        return appointmentService.cancelForPatient(userId, appointmentId);
    }
}