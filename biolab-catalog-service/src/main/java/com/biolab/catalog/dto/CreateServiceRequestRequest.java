package com.biolab.catalog.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreateServiceRequestRequest(
    @NotNull UUID serviceId,
    String sampleType, String timeline, String requirements, String priority
) {}
