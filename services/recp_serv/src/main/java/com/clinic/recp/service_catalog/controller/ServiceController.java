package com.clinic.recp.service_catalog.controller;

import com.clinic.recp.service_catalog.dto.ServiceRequest;
import com.clinic.recp.service_catalog.dto.ServiceResponse;
import com.clinic.recp.service_catalog.service.ServiceCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    @GetMapping
    public List<ServiceResponse> findAll() {
        return serviceCatalogService.findAllActive();
    }

    @GetMapping("/{id}")
    public ServiceResponse findById(@PathVariable Long id) {
        return serviceCatalogService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ServiceResponse create(@Valid @RequestBody ServiceRequest request) {
        return serviceCatalogService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ServiceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request
    ) {
        return serviceCatalogService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public void deactivate(@PathVariable Long id) {
        serviceCatalogService.deactivate(id);
    }
}