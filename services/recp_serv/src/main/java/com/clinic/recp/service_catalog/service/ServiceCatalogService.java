package com.clinic.recp.service_catalog.service;

import com.clinic.recp.service_catalog.dto.ServiceRequest;
import com.clinic.recp.service_catalog.dto.ServiceResponse;
import com.clinic.recp.service_catalog.entity.Service;
import com.clinic.recp.service_catalog.repository.ServiceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;

    public List<ServiceResponse> findAllActive() {
        return serviceRepository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ServiceResponse findById(Long id) {
        return toResponse(getService(id));
    }

    @Transactional
    public ServiceResponse create(ServiceRequest request) {
        Service service = new Service();
        applyRequest(service, request);

        return toResponse(serviceRepository.save(service));
    }

    @Transactional
    public ServiceResponse update(Long id, ServiceRequest request) {
        Service service = getService(id);
        applyRequest(service, request);

        return toResponse(service);
    }

    @Transactional
    public void deactivate(Long id) {
        Service service = getService(id);
        service.setIsActive(false);
    }

    private Service getService(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Service not found: " + id));
    }

    private void applyRequest(Service service, ServiceRequest request) {
        service.setName(request.name());
        service.setDescription(request.description());
        service.setPrice(request.price());
        service.setDurationMin(request.durationMin());
        service.setBufferMin(request.bufferMin());
    }

    private ServiceResponse toResponse(Service service) {
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