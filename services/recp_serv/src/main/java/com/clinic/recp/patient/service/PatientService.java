package com.clinic.recp.patient.service;

import com.clinic.recp.patient.dto.CreatePatientRequest;
import com.clinic.recp.patient.dto.InternalCreatePatientRequest;
import com.clinic.recp.patient.dto.PatientResponse;
import com.clinic.recp.patient.dto.UpdatePatientRequest;
import com.clinic.recp.patient.entity.Patient;
import com.clinic.recp.patient.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;

    @Transactional
    public PatientResponse create(CreatePatientRequest request) {
        Patient patient = new Patient();

        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setPesel(request.pesel());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setNotes(request.notes());

        return toResponse(patientRepository.save(patient));
    }

    public List<PatientResponse> findAll(String search) {
        List<Patient> patients;

        if (search == null || search.isBlank()) {
            patients = patientRepository.findAll();
        } else {
            patients = patientRepository.search(search.trim());
        }

        return patients.stream()
                .map(this::toResponse)
                .toList();
    }

    public PatientResponse findById(Long id) {
        return toResponse(getPatient(id));
    }

    @Transactional
    public PatientResponse update(Long id, UpdatePatientRequest request) {
        Patient patient = getPatient(id);

        if (request.firstName() != null) {
            patient.setFirstName(request.firstName());
        }

        if (request.lastName() != null) {
            patient.setLastName(request.lastName());
        }

        if (request.pesel() != null) {
            patient.setPesel(request.pesel());
        }

        if (request.dateOfBirth() != null) {
            patient.setDateOfBirth(request.dateOfBirth());
        }

        if (request.phone() != null) {
            patient.setPhone(request.phone());
        }

        if (request.email() != null) {
            patient.setEmail(request.email());
        }

        if (request.notes() != null) {
            patient.setNotes(request.notes());
        }

        return toResponse(patient);
    }

    @Transactional
    public PatientResponse verify(Long id) {
        Patient patient = getPatient(id);

        patient.setVerified(true);

        return toResponse(patient);
    }

    @Transactional
    public PatientResponse createForUser(InternalCreatePatientRequest request) {
        return patientRepository.findByUserId(request.userId())
                .map(this::toResponse)
                .orElseGet(() -> {
                    Patient patient = new Patient();

                    patient.setUserId(request.userId());
                    patient.setFirstName(request.firstName());
                    patient.setLastName(request.lastName());
                    patient.setEmail(request.email());
                    patient.setPhone(request.phone());

                    return toResponse(patientRepository.save(patient));
                });
    }

    public PatientResponse findByUserId(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Patient not found for user ID: " + userId
                ));

        return toResponse(patient);
    }

    private Patient getPatient(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Patient not found with ID: " + id
                ));
    }

    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getPesel(),
                patient.getDateOfBirth(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getNotes(),
                patient.getUserId(),
                patient.isVerified(),
                patient.getCreatedAt()
        );
    }
}