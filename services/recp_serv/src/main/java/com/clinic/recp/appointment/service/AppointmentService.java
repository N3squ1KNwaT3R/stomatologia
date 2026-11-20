package com.clinic.recp.appointment.service;

import com.clinic.recp.appointment.dto.*;
import com.clinic.recp.appointment.entity.Appointment;
import com.clinic.recp.appointment.entity.AppointmentSource;
import com.clinic.recp.appointment.entity.AppointmentStatus;
import com.clinic.recp.appointment.exception.AppointmentConflictException;
import com.clinic.recp.appointment.repository.AppointmentRepository;
import com.clinic.recp.doctor.entity.Doctor;
import com.clinic.recp.doctor.repository.DoctorRepository;
import com.clinic.recp.doctor_working_hours.entity.DoctorWorkingHours;
import com.clinic.recp.doctor_working_hours.repository.DoctorWorkingHoursRepository;
import com.clinic.recp.patient.entity.Patient;
import com.clinic.recp.patient.repository.PatientRepository;
import com.clinic.recp.service_catalog.entity.Service;
import com.clinic.recp.service_catalog.repository.ServiceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import com.clinic.recp.appointment.dto.AppointmentSlotResponse;
import java.time.LocalDateTime;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
public class AppointmentService {

    private static final ZoneId CLINIC_ZONE =
            ZoneId.of("Europe/Warsaw");

    private static final List<AppointmentStatus> BLOCKING_STATUSES =
            List.of(
                    AppointmentStatus.PENDING,
                    AppointmentStatus.SCHEDULED
            );

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ServiceRepository serviceRepository;
    private final DoctorWorkingHoursRepository workingHoursRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            ServiceRepository serviceRepository,
            DoctorWorkingHoursRepository workingHoursRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.serviceRepository = serviceRepository;
        this.workingHoursRepository = workingHoursRepository;
    }

    @Transactional
    public AppointmentResponse createForReception(
            Long userId,
            ReceptionCreateAppointmentRequest request
    ) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Patient not found"));

        Doctor doctor = getDoctor(request.doctorId());
        Service service = getService(request.serviceId());

        validateDoctorService(doctor, service);

        int duration = request.durationMin() != null
                ? request.durationMin()
                : service.getDurationMin();

        int buffer = request.bufferMin() != null
                ? request.bufferMin()
                : service.getBufferMin();

        String room = request.room() != null
                ? request.room()
                : doctor.getRoom();

        return createAppointment(
                userId,
                patient,
                doctor,
                service,
                request.startAt(),
                duration,
                buffer,
                room,
                request.comment(),
                AppointmentStatus.SCHEDULED,
                AppointmentSource.RECEPTION
        );
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getDoctorAppointments(
            Long userId,
            LocalDate date
    ) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor profile not found"));

        Instant from = date.atStartOfDay(CLINIC_ZONE).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();

        return appointmentRepository
                .findAllByDoctorIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
                        doctor.getId(), from, to
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AppointmentResponse createForPatient(
            Long userId,
            PatientCreateAppointmentRequest request
    ) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Patient profile not found"
                        ));

        Doctor doctor = getDoctor(request.doctorId());
        Service service = getService(request.serviceId());

        validateDoctorService(doctor, service);

        if (!Boolean.TRUE.equals(service.getIsActive())) {
            throw new IllegalArgumentException(
                    "Service is not active"
            );
        }

        return createAppointment(
                userId,
                patient,
                doctor,
                service,
                request.startAt(),
                service.getDurationMin(),
                service.getBufferMin(),
                doctor.getRoom(),
                request.comment(),
                AppointmentStatus.PENDING,
                AppointmentSource.WEBSITE
        );
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Patient profile not found"));

        return appointmentRepository
                .findAllByPatientIdOrderByStartAtDesc(patient.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AppointmentResponse cancelForPatient(Long userId, Long appointmentId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Patient profile not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new EntityNotFoundException("Appointment not found");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new AppointmentConflictException("Appointment cannot be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(Instant.now());

        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsForReception(
            Long doctorId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AppointmentStatus status
    ) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom must not be after dateTo");
        }

        Instant from = dateFrom == null
                ? null
                : dateFrom.atStartOfDay(CLINIC_ZONE).toInstant();

        Instant to = dateTo == null
                ? null
                : dateTo.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();

        Specification<Appointment> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayLiсдуфкst<>();

            if (doctorId != null) {
                predicates.add(cb.equal(root.get("doctor").get("id"), doctorId));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<Instant>get("startAt"), from
                ));
            }

            if (to != null) {
                predicates.add(cb.lessThan(
                        root.<Instant>get("startAt"), to
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return appointmentRepository.findAll(
                        specification,
                        Sort.by(Sort.Direction.ASC, "startAt")
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AppointmentResponse confirmAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new AppointmentConflictException("Only pending appointments can be confirmed");
        }

        appointment.setStatus(AppointmentStatus.SCHEDULED);

        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentSlotResponse> getAvailableSlots(
            Long doctorId,
            Long serviceId,
            LocalDate date
    ) {
        Doctor doctor = getDoctor(doctorId);
        Service service = getService(serviceId);

        if (!Boolean.TRUE.equals(service.getIsActive())) {
            throw new IllegalArgumentException("Service is not active");
        }

        validateDoctorService(doctor, service);

        Instant dayStart = date.atStartOfDay(CLINIC_ZONE).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();

        List<Appointment> occupiedAppointments =
                appointmentRepository.findBlockingAppointments(
                        doctorId, BLOCKING_STATUSES, dayStart, dayEnd
                );

        List<DoctorWorkingHours> workingHours =
                workingHoursRepository.findAllByDoctorIdOrderByWeekdayAscStartTimeAsc(doctorId);

        short weekday = (short) (date.getDayOfWeek().getValue() % 7);

        int duration = service.getDurationMin();
        int buffer = service.getBufferMin();

        List<AppointmentSlotResponse> slots = new java.util.ArrayList<>();

        for (DoctorWorkingHours hours : workingHours) {
            if (!hours.getWeekday().equals(weekday)) {
                continue;
            }

            LocalDateTime candidate = date.atStartOfDay();

            while (candidate.toLocalDate().equals(date)
                    && !candidate.toLocalTime().isAfter(hours.getEndTime())) {
                LocalTime candidateTime = candidate.toLocalTime();

                if (candidateTime.isBefore(hours.getStartTime())) {
                    candidate = candidate.plusMinutes(15);
                    continue;
                }

                LocalDateTime blockedUntilLocal = candidate.plusMinutes(duration + buffer);

                if (blockedUntilLocal.toLocalDate().equals(date)
                        && !blockedUntilLocal.toLocalTime().isAfter(hours.getEndTime())) {

                    Instant startAt = candidate.atZone(CLINIC_ZONE).toInstant();
                    Instant endAt = startAt.plusSeconds(duration * 60L);
                    Instant blockedUntil = endAt.plusSeconds(buffer * 60L);

                    boolean isFuture = startAt.isAfter(Instant.now());

                    boolean isOccupied = occupiedAppointments.stream().anyMatch(appointment ->
                            appointment.getStartAt().isBefore(blockedUntil)
                                    && appointment.getBlockedUntil().isAfter(startAt)
                    );

                    if (isFuture && !isOccupied) {
                        slots.add(new AppointmentSlotResponse(
                                startAt.atZone(CLINIC_ZONE).toOffsetDateTime(),
                                endAt.atZone(CLINIC_ZONE).toOffsetDateTime()
                        ));
                    }
                }

                candidate = candidate.plusMinutes(15);
            }
        }

        return slots;
    }

    @Transactional(readOnly = true)
    public InternalAppointmentResponse getInternalAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        return new InternalAppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getUserId(),
                appointment.getService().getId(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getBlockedUntil(),
                appointment.getStatus(),
                appointment.getSource()
        );
    }

    @Transactional
    public void completeAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            return;
        }

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new AppointmentConflictException(
                    "Only scheduled appointments can be completed"
            );
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));

        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new AppointmentConflictException("Appointment cannot be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(Instant.now());

        return toResponse(appointmentRepository.save(appointment));
    }

    private AppointmentResponse createAppointment(
            Long userId,
            Patient patient,
            Doctor doctor,
            Service service,
            OffsetDateTime requestedStart,
            int duration,
            int buffer,
            String room,
            String comment,
            AppointmentStatus status,
            AppointmentSource source
    ) {
        Instant startAt = requestedStart.toInstant();

        if (!startAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Appointment must be in the future"
            );
        }

        if (duration <= 0 || buffer < 0) {
            throw new IllegalArgumentException(
                    "Invalid appointment duration or buffer"
            );
        }

        ZonedDateTime localStart = startAt.atZone(CLINIC_ZONE);

        if (localStart.getMinute() % 15 != 0
                || localStart.getSecond() != 0
                || localStart.getNano() != 0) {
            throw new IllegalArgumentException(
                    "Appointment must start on a 15-minute boundary"
            );
        }

        Instant endAt = startAt.plusSeconds(duration * 60L);
        Instant blockedUntil = endAt.plusSeconds(buffer * 60L);

        validateWorkingHours(doctor.getId(), startAt, blockedUntil);

        boolean occupied = appointmentRepository
                .existsByDoctorIdAndStatusInAndStartAtLessThanAndBlockedUntilGreaterThan(
                        doctor.getId(),
                        BLOCKING_STATUSES,
                        blockedUntil,
                        startAt
                );

        if (occupied) {
            throw new AppointmentConflictException(
                    "Doctor already has an appointment at this time"
            );
        }

        Appointment appointment = new Appointment();

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setService(service);
        appointment.setStartAt(startAt);
        appointment.setDurationMin(duration);
        appointment.setBufferMin(buffer);
        appointment.setEndAt(endAt);
        appointment.setBlockedUntil(blockedUntil);
        appointment.setStatus(status);
        appointment.setSource(source);
        appointment.setRoom(room);
        appointment.setComment(comment);
        appointment.setCreatedBy(userId);

        return toResponse(appointmentRepository.save(appointment));
    }

    private void validateWorkingHours(
            Long doctorId,
            Instant startAt,
            Instant blockedUntil
    ) {
        ZonedDateTime localStart = startAt.atZone(CLINIC_ZONE);
        ZonedDateTime localBlockedUntil =
                blockedUntil.atZone(CLINIC_ZONE);

        short weekday = (short)
                (localStart.getDayOfWeek().getValue() % 7);

        boolean fitsWorkingHours = workingHoursRepository
                .findAllByDoctorIdOrderByWeekdayAscStartTimeAsc(doctorId)
                .stream()
                .filter(hours -> hours.getWeekday().equals(weekday))
                .anyMatch(hours ->
                        !localStart.toLocalTime().isBefore(hours.getStartTime())
                                && !localBlockedUntil.toLocalTime()
                                .isAfter(hours.getEndTime())
                                && localStart.toLocalDate()
                                .equals(localBlockedUntil.toLocalDate())
                );

        if (!fitsWorkingHours) {
            throw new AppointmentConflictException(
                    "Appointment is outside doctor's working hours"
            );
        }
    }

    private void validateDoctorService(
            Doctor doctor,
            Service service
    ) {
        boolean providesService = doctor.getServices()
                .stream()
                .anyMatch(doctorService ->
                        doctorService.getId().equals(service.getId())
                );

        if (!providesService) {
            throw new IllegalArgumentException(
                    "Doctor does not provide this service"
            );
        }
    }

    private Doctor getDoctor(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Doctor not found"));
    }

    private Service getService(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Service not found"));
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getDoctor().getId(),
                appointment.getService().getId(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getBlockedUntil(),
                appointment.getDurationMin(),
                appointment.getBufferMin(),
                appointment.getStatus(),
                appointment.getSource(),
                appointment.getRoom(),
                appointment.getComment(),
                appointment.getCancelledAt()
        );
    }
}