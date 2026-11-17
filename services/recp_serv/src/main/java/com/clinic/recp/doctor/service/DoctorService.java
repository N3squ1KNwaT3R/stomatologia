package com.clinic.recp.doctor.service;

import com.clinic.recp.doctor.dto.CreateDoctorRequest;
import com.clinic.recp.doctor.dto.DoctorResponse;
import com.clinic.recp.doctor.dto.UpdateDoctorRequest;
import com.clinic.recp.doctor.entity.Doctor;
import com.clinic.recp.doctor.repository.DoctorRepository;
import com.clinic.recp.service_catalog.dto.ServiceResponse;
import com.clinic.recp.service_catalog.entity.Service;
import com.clinic.recp.service_catalog.repository.ServiceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final ServiceRepository serviceRepository;

    public List<DoctorResponse> findAll() {
        return doctorRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DoctorResponse findById(Long id) {
        Doctor doctor = getDoctor(id);
        return toResponse(doctor);
    }

    public List<ServiceResponse> findServices(Long doctorId) {
        Doctor doctor = getDoctor(doctorId);

        return doctor.getServices()
                .stream()
                .map(this::toServiceResponse)
                .toList();
    }

    @Transactional
    public DoctorResponse create(CreateDoctorRequest request) {
        if (doctorRepository.existsByUserId(request.userId())) {
            throw new IllegalArgumentException(
                    "A doctor with this user ID already exists"
            );
        }

        Doctor doctor = new Doctor();
        doctor.setUserId(request.userId());
        doctor.setSpecialization(request.specialization());
        doctor.setRoom(request.room());

        return toResponse(doctorRepository.save(doctor));
    }

    @Transactional
    public DoctorResponse update(Long id, UpdateDoctorRequest request) {
        Doctor doctor = getDoctor(id);

        doctor.setSpecialization(request.specialization());
        doctor.setRoom(request.room());

        return toResponse(doctor);
    }

    @Transactional
    public List<ServiceResponse> updateServices(
            Long doctorId,
            List<Long> serviceIds
    ) {
        Doctor doctor = getDoctor(doctorId);

        Set<Long> uniqueIds = new HashSet<>(serviceIds);

        if (uniqueIds.size() != serviceIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate service IDs are not allowed"
            );
        }

        List<Service> services = serviceRepository.findAllById(serviceIds);

        if (services.size() != uniqueIds.size()) {
            throw new IllegalArgumentException(
                    "One or more services do not exist"
            );
        }

        boolean hasInactiveService = services.stream()
                .anyMatch(service -> !Boolean.TRUE.equals(service.getIsActive()));

        if (hasInactiveService) {
            throw new IllegalArgumentException(
                    "Inactive services cannot be assigned to a doctor"
            );
        }

        doctor.getServices().clear();
        doctor.getServices().addAll(services);

        return doctor.getServices()
                .stream()
                .map(this::toServiceResponse)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Doctor doctor = getDoctor(id);
        doctorRepository.delete(doctor);
    }

    private Doctor getDoctor(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Doctor not found with ID: " + id
                ));
    }

    private DoctorResponse toResponse(Doctor doctor) {
        List<ServiceResponse> services = doctor.getServices()
                .stream()
                .map(this::toServiceResponse)
                .toList();

        return new DoctorResponse(
                doctor.getId(),
                doctor.getUserId(),
                doctor.getSpecialization(),
                doctor.getRoom(),
                services
        );
    }

    private ServiceResponse toServiceResponse(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                service.getDurationMin(),
                service.getBufferMin(),
                service.getIsActive()
        );
    }
}