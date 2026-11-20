package com.clinic.recp.appointment.controller;

import com.clinic.recp.appointment.dto.AppointmentSlotResponse;
import com.clinic.recp.appointment.service.AppointmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/appointments/slots")
@PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST')")
public class AppointmentSlotsController {

    private final AppointmentService appointmentService;

    public AppointmentSlotsController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<AppointmentSlotResponse> getAvailableSlots(
            @RequestParam(name = "doctor_id") Long doctorId,
            @RequestParam(name = "service_id") Long serviceId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return appointmentService.getAvailableSlots(doctorId, serviceId, date);
    }
}