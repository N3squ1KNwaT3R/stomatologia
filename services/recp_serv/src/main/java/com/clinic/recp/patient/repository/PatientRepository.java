package com.clinic.recp.patient.repository;

import com.clinic.recp.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUserId(Long userId);

    @Query("""
            SELECT p FROM Patient p
            WHERE LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(p.phone, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR COALESCE(p.pesel, '') LIKE CONCAT('%', :search, '%')
            ORDER BY p.id
            """)
    List<Patient> search(@Param("search") String search);
}