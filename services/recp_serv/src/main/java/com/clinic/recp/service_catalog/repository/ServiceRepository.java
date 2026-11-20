package com.clinic.recp.service_catalog.repository;

import com.clinic.recp.service_catalog.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findAllByIsActiveTrueOrderByNameAsc();
}