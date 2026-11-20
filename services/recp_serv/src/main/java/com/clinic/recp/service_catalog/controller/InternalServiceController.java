package com.clinic.recp.service_catalog.controller;

import com.clinic.recp.service_catalog.dto.InternalServiceResponse;
import com.clinic.recp.service_catalog.service.ServiceCatalogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/services")
@PreAuthorize("hasRole('INTERNAL')")
public class InternalServiceController {

    private final ServiceCatalogService serviceCatalogService;

    public InternalServiceController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping("/{id}")
    public InternalServiceResponse getService(@PathVariable Long id) {
        return serviceCatalogService.getInternalService(id);
    }
}