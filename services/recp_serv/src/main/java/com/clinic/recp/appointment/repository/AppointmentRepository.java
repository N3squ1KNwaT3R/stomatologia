package com.clinic.recp.appointment.repository;

import com.clinic.recp.appointment.entity.Appointment;
import com.clinic.recp.appointment.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long>,
        JpaSpecificationExecutor<Appointment> {

    List<Appointment> findAllByPatientIdOrderByStartAtDesc(Long patientId);

    List<Appointment> findAllByDoctorIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
            Long doctorId,
            Instant from,
            Instant to
    );

    List<Appointment> findAllByDoctorIdAndStartAtGreaterThanEqualAndStartAtLessThanAndStatusOrderByStartAtAsc(
            Long doctorId,
            Instant from,
            Instant to,
            AppointmentStatus status
    );

    boolean existsByDoctorIdAndStatusInAndStartAtLessThanAndBlockedUntilGreaterThan(
            Long doctorId,
            List<AppointmentStatus> statuses,
            Instant blockedUntil,
            Instant startAt
    );

    @Query("""
    SELECT a FROM Appointment a
    WHERE a.doctor.id = :doctorId
      AND a.status IN :statuses
      AND a.startAt < :rangeEnd
      AND a.blockedUntil > :rangeStart
    ORDER BY a.startAt ASC
    """)
    List<Appointment> findBlockingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("statuses") List<AppointmentStatus> statuses,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd
    );

    @Query("""
    SELECT a FROM Appointment a
    WHERE (:doctorId IS NULL OR a.doctor.id = :doctorId)
      AND (:from IS NULL OR a.startAt >= :from)
      AND (:to IS NULL OR a.startAt < :to)
      AND (:status IS NULL OR a.status = :status)
    ORDER BY a.startAt ASC
    """)
    List<Appointment> findForReception(
            @Param("doctorId") Long doctorId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("status") AppointmentStatus status
    );
}