package com.biolab.auth.service;

import com.biolab.auth.dto.request.RoleCreateRequest;
import com.biolab.auth.dto.request.RoleUpdateRequest;
import com.biolab.auth.dto.response.RoleResponse;
import java.util.List;
import java.util.UUID;

/** CRUD operations for roles. */
public interface RoleService {
    RoleResponse create(RoleCreateRequest request);
    List<RoleResponse> getAll();
    RoleResponse getById(UUID id);
    RoleResponse getByName(String name);
    RoleResponse update(UUID id, RoleUpdateRequest request);
    void delete(UUID id);
}
