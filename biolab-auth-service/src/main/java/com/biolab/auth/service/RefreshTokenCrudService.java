package com.biolab.auth.service;

import com.biolab.auth.dto.response.RefreshTokenInfoResponse;
import java.util.List;
import java.util.UUID;
public interface RefreshTokenCrudService {
    List<RefreshTokenInfoResponse> getActiveByUserId(UUID userId);
    void revokeAllByUserId(UUID userId);
}
