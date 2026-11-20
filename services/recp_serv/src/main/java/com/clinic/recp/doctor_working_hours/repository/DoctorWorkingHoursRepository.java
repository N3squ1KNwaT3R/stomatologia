package com.clinic.recp.doctor_working_hours.repository;

import com.clinic.recp.doctor_working_hours.entity.DoctorWorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorWorkingHoursRepository
        extends JpaRepository<DoctorWorkingHours, Long> {

    List<DoctorWorkingHours> findAllByDoctorIdOrderByWeekdayAscStartTimeAsc(
            Long doctorId
    );

    void deleteAllByDoctorId(Long doctorId);
}