package com.clinic.recp.appointment.controller;

import com.clinic.recp.appointment.dto.AppointmentResponse;
import com.clinic.recp.appointment.dto.ReceptionCreateAppointmentRequest;
import com.clinic.recp.appointment.entity.AppointmentStatus;
import com.clinic.recp.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@PreAuthorize("hasRole('RECEPTIONIST')")
public class ReceptionAppointmentController {

    private final AppointmentService appointmentService;

    public ReceptionAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createAppointment(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReceptionCreateAppointmentRequest request
    ) {
        return appointmentService.createForReception(userId, request);
    }

    @GetMapping
    public List<AppointmentResponse> getAppointments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(name = "date_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "date_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) AppointmentStatus status
    ) {
        return appointmentService.getAppointmentsForReception(
                doctorId, dateFrom, dateTo, status
        );
    }

    @PostMapping("/{appointmentId}/confirm")
    public AppointmentResponse confirmAppointment(
            @PathVariable Long appointmentId
    ) {
        return appointmentService.confirmAppointment(appointmentId);
    }

    @PostMapping("/{appointmentId}/cancel")
    public AppointmentResponse cancelAppointment(
            @PathVariable Long appointmentId
    ) {
        return appointmentService.cancelAppointment(appointmentId);
    }
}