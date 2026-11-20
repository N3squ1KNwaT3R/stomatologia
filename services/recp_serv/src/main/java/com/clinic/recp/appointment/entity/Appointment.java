package com.clinic.recp.appointment.entity;

import com.clinic.recp.doctor.entity.Doctor;
import com.clinic.recp.patient.entity.Patient;
import com.clinic.recp.service_catalog.entity.Service;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "appointments")
@Getter
@Setter
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "duration_min", nullable = false)
    private Integer durationMin;

    @Column(name = "buffer_min", nullable = false)
    private Integer bufferMin = 0;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "blocked_until", nullable = false)
    private Instant blockedUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentSource source;

    @Column(length = 20)
    private String room;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}