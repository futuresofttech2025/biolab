package com.biolab.auth.service;

import com.biolab.auth.dto.response.PasswordHistoryResponse;
import java.util.List;
import java.util.UUID;
public interface PasswordHistoryService {
    List<PasswordHistoryResponse> getByUserId(UUID userId);
}
