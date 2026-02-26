package com.biolab.auth.service;

import com.biolab.auth.dto.request.PermissionCreateRequest;
import com.biolab.auth.dto.request.PermissionUpdateRequest;
import com.biolab.auth.dto.response.PermissionResponse;
import java.util.List;
import java.util.UUID;

/** CRUD operations for permissions. */
public interface PermissionService {
    PermissionResponse create(PermissionCreateRequest request);
    List<PermissionResponse> getAll();
    PermissionResponse getById(UUID id);
    List<PermissionResponse> getByModule(String module);
    PermissionResponse update(UUID id, PermissionUpdateRequest request);
    void delete(UUID id);
}
