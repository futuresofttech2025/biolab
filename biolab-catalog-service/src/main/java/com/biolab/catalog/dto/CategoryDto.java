package com.biolab.catalog.dto;

import java.util.UUID;

public record CategoryDto(UUID id, String name, String slug, String description, String icon, Boolean isActive) {}
