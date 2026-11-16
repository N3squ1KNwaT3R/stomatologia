package com.clinic.recp.doctor.repository;

import com.clinic.recp.doctor.entity.Doctor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    boolean existsByUserId(Long userId);

    @EntityGraph(attributePaths = "services")
    List<Doctor> findAllByOrderByIdAsc();

    @Override
    @EntityGraph(attributePaths = "services")
    Optional<Doctor> findById(Long id);
}
