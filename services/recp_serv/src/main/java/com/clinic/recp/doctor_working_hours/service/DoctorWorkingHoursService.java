package com.clinic.recp.doctor_working_hours.service;

import com.clinic.recp.doctor.entity.Doctor;
import com.clinic.recp.doctor.repository.DoctorRepository;
import com.clinic.recp.doctor_working_hours.dto.WorkingHoursRequest;
import com.clinic.recp.doctor_working_hours.dto.WorkingHoursResponse;
import com.clinic.recp.doctor_working_hours.entity.DoctorWorkingHours;
import com.clinic.recp.doctor_working_hours.repository.DoctorWorkingHoursRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DoctorWorkingHoursService {

    private final DoctorRepository doctorRepository;
    private final DoctorWorkingHoursRepository workingHoursRepository;

    public DoctorWorkingHoursService(
            DoctorRepository doctorRepository,
            DoctorWorkingHoursRepository workingHoursRepository
    ) {
        this.doctorRepository = doctorRepository;
        this.workingHoursRepository = workingHoursRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkingHoursResponse> getWorkingHours(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new EntityNotFoundException("Doctor not found");
        }

        return workingHoursRepository
                .findAllByDoctorIdOrderByWeekdayAscStartTimeAsc(doctorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<WorkingHoursResponse> replaceWorkingHours(
            Long doctorId,
            List<WorkingHoursRequest> requests
    ) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Doctor not found"));

        validateIntervals(requests);

        workingHoursRepository.deleteAllByDoctorId(doctorId);

        List<DoctorWorkingHours> entities = requests.stream()
                .map(request -> {
                    DoctorWorkingHours entity = new DoctorWorkingHours();
                    entity.setDoctor(doctor);
                    entity.setWeekday(request.weekday());
                    entity.setStartTime(request.startTime());
                    entity.setEndTime(request.endTime());
                    return entity;
                })
                .toList();

        return workingHoursRepository.saveAll(entities)
                .stream()
                .sorted(
                        Comparator.comparing(DoctorWorkingHours::getWeekday)
                                .thenComparing(DoctorWorkingHours::getStartTime)
                )
                .map(this::toResponse)
                .toList();
    }

    private void validateIntervals(List<WorkingHoursRequest> requests) {
        if (requests == null) {
            throw new IllegalArgumentException("Working hours list cannot be null");
        }

        for (WorkingHoursRequest request : requests) {
            if (request.startTime() == null || request.endTime() == null
                    || request.weekday() == null) {
                throw new IllegalArgumentException(
                        "Weekday, start time and end time are required"
                );
            }

            if (!request.startTime().isBefore(request.endTime())) {
                throw new IllegalArgumentException(
                        "Start time must be before end time"
                );
            }
        }

        List<WorkingHoursRequest> sorted = new ArrayList<>(requests);

        sorted.sort(
                Comparator.comparing(WorkingHoursRequest::weekday)
                        .thenComparing(WorkingHoursRequest::startTime)
        );

        for (int i = 1; i < sorted.size(); i++) {
            WorkingHoursRequest previous = sorted.get(i - 1);
            WorkingHoursRequest current = sorted.get(i);

            if (previous.weekday().equals(current.weekday())
                    && current.startTime().isBefore(previous.endTime())) {
                throw new IllegalArgumentException(
                        "Working hours cannot overlap"
                );
            }
        }
    }

    private WorkingHoursResponse toResponse(DoctorWorkingHours entity) {
        return new WorkingHoursResponse(
                entity.getId(),
                entity.getDoctor().getId(),
                entity.getWeekday(),
                entity.getStartTime(),
                entity.getEndTime()
        );
    }
}