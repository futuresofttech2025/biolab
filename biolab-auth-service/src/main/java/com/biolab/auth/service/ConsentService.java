package com.biolab.auth.service;

import com.biolab.auth.dto.request.ConsentRequest;
import com.biolab.auth.dto.response.ConsentRecordResponse;
import java.util.List;
import java.util.UUID;
public interface ConsentService {
    ConsentRecordResponse grant(UUID userId, ConsentRequest request, String ipAddress);
    ConsentRecordResponse revoke(UUID userId, String consentType);
    List<ConsentRecordResponse> getByUserId(UUID userId);
}
