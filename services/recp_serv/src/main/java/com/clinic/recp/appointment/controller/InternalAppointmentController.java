package com.clinic.recp.appointment.controller;

import com.clinic.recp.appointment.dto.InternalAppointmentResponse;
import com.clinic.recp.appointment.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/appointments")
@PreAuthorize("hasRole('INTERNAL')")
public class InternalAppointmentController {

    private final AppointmentService appointmentService;

    public InternalAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/{id}")
    public InternalAppointmentResponse getAppointment(@PathVariable Long id) {
        return appointmentService.getInternalAppointment(id);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeAppointment(@PathVariable Long id) {
        appointmentService.completeAppointment(id);
        return ResponseEntity.noContent().build();
    }
}