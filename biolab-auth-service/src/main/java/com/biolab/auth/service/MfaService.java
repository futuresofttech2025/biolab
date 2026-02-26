package com.biolab.auth.service;

import com.biolab.auth.dto.response.MfaSettingsResponse;
import java.util.List;
import java.util.UUID;
public interface MfaService {
    List<MfaSettingsResponse> getByUserId(UUID userId);
    void disable(UUID userId, String mfaType);
}
