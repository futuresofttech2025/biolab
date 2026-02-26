package com.biolab.auth.service;

import com.biolab.auth.dto.response.JwtTokenBlacklistResponse;
import java.util.List;
import java.util.UUID;
public interface TokenBlacklistService {
    boolean isBlacklisted(String jti);
    List<JwtTokenBlacklistResponse> getByUserId(UUID userId);
}
