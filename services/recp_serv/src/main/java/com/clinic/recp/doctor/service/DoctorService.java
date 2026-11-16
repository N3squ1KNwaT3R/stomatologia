package com.clinic.recp.doctor.service;

import com.clinic.recp.doctor.dto.CreateDoctorRequest;
import com.clinic.recp.doctor.dto.DoctorResponse;
import com.clinic.recp.doctor.dto.UpdateDoctorRequest;
import com.clinic.recp.doctor.entity.Doctor;
import com.clinic.recp.doctor.repository.DoctorRepository;
import com.clinic.recp.service_catalog.dto.ServiceResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;

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
                .map(service -> new ServiceResponse(
                        service.getId(),
                        service.getName(),
                        service.getDescription(),
                        service.getPrice(),
                        service.getDurationMin(),
                        service.getBufferMin(),
                        service.getIsActive()
                ))
                .toList();

        return new DoctorResponse(
                doctor.getId(),
                doctor.getUserId(),
                doctor.getSpecialization(),
                doctor.getRoom(),
                services
        );
    }
}