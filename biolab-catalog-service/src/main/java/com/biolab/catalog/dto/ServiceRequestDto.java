package com.biolab.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record ServiceRequestDto(UUID id, UUID serviceId, String serviceName, String serviceCat,
                                 UUID buyerId, UUID buyerOrgId, String sampleType,
                                 String timeline, String requirements, String priority,
                                 String status, Instant createdAt) {}
