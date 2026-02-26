package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.RefreshTokenInfoResponse;
import com.biolab.auth.repository.RefreshTokenRepository;
import com.biolab.auth.service.RefreshTokenCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional
public class RefreshTokenCrudServiceImpl implements RefreshTokenCrudService {
    private final RefreshTokenRepository repo;
    @Override @Transactional(readOnly=true) public List<RefreshTokenInfoResponse> getActiveByUserId(UUID userId) {
        return repo.findByUserIdAndIsRevokedFalse(userId).stream().map(t -> RefreshTokenInfoResponse.builder()
                .id(t.getId()).tokenFamily(t.getTokenFamily()).generation(t.getGeneration())
                .isRevoked(t.getIsRevoked()).revokedReason(t.getRevokedReason()!=null?t.getRevokedReason().name():null)
                .ipAddress(t.getIpAddress()).issuedAt(t.getIssuedAt()).expiresAt(t.getExpiresAt()).build()).toList();
    }
    @Override public void revokeAllByUserId(UUID userId) { repo.revokeAllByUserId(userId); }
}
