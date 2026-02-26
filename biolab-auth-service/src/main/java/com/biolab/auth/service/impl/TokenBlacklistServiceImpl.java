package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.JwtTokenBlacklistResponse;
import com.biolab.auth.repository.JwtTokenBlacklistRepository;
import com.biolab.auth.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class TokenBlacklistServiceImpl implements TokenBlacklistService {
    private final JwtTokenBlacklistRepository repo;
    @Override public boolean isBlacklisted(String jti) { return repo.existsByJti(jti); }
    @Override public List<JwtTokenBlacklistResponse> getByUserId(UUID userId) {
        return repo.findByUserId(userId).stream().map(t -> JwtTokenBlacklistResponse.builder()
                .id(t.getId()).jti(t.getJti()).tokenType(t.getTokenType().name())
                .expiresAt(t.getExpiresAt()).blacklistedAt(t.getBlacklistedAt()).reason(t.getReason()).build()).toList();
    }
}
