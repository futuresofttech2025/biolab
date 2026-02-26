package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.ConsentRequest;
import com.biolab.auth.dto.response.ConsentRecordResponse;
import com.biolab.auth.entity.*;
import com.biolab.auth.entity.enums.ConsentType;
import com.biolab.auth.exception.ResourceNotFoundException;
import com.biolab.auth.repository.*;
import com.biolab.auth.service.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional
public class ConsentServiceImpl implements ConsentService {
    private final ConsentRecordRepository repo;
    private final UserRepository userRepo;
    @Override public ConsentRecordResponse grant(UUID userId, ConsentRequest req, String ip) {
        User user = userRepo.findById(userId).orElseThrow(()->new ResourceNotFoundException("User","id",userId));
        ConsentType type = ConsentType.valueOf(req.getConsentType());
        ConsentRecord cr = repo.findByUserIdAndConsentType(userId, type)
                .map(existing -> { existing.setGrantedAt(Instant.now()); existing.setRevokedAt(null);
                    existing.setIpAddress(ip); existing.setVersion(req.getVersion()!=null?req.getVersion():"1.0"); return existing; })
                .orElse(ConsentRecord.builder().user(user).consentType(type).ipAddress(ip)
                        .version(req.getVersion()!=null?req.getVersion():"1.0").build());
        return toResp(repo.save(cr));
    }
    @Override public ConsentRecordResponse revoke(UUID userId, String consentType) {
        ConsentRecord cr = repo.findByUserIdAndConsentType(userId, ConsentType.valueOf(consentType))
                .orElseThrow(()->new ResourceNotFoundException("Consent","type",consentType));
        cr.setRevokedAt(Instant.now()); return toResp(repo.save(cr));
    }
    @Override @Transactional(readOnly=true) public List<ConsentRecordResponse> getByUserId(UUID userId) {
        return repo.findByUserId(userId).stream().map(this::toResp).toList();
    }
    private ConsentRecordResponse toResp(ConsentRecord c) {
        return ConsentRecordResponse.builder().id(c.getId()).consentType(c.getConsentType().name())
                .grantedAt(c.getGrantedAt()).revokedAt(c.getRevokedAt()).ipAddress(c.getIpAddress()).version(c.getVersion()).build();
    }
}
