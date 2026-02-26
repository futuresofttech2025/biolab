package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.MfaSettingsResponse;
import com.biolab.auth.entity.enums.MfaType;
import com.biolab.auth.exception.ResourceNotFoundException;
import com.biolab.auth.repository.MfaSettingsRepository;
import com.biolab.auth.service.MfaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional
public class MfaServiceImpl implements MfaService {
    private final MfaSettingsRepository repo;
    @Override @Transactional(readOnly=true) public List<MfaSettingsResponse> getByUserId(UUID userId) {
        return repo.findByUserId(userId).stream().map(m -> MfaSettingsResponse.builder()
                .id(m.getId()).mfaType(m.getMfaType().name()).isEnabled(m.getIsEnabled()).verifiedAt(m.getVerifiedAt()).build()).toList();
    }
    @Override public void disable(UUID userId, String mfaType) {
        var m = repo.findByUserIdAndMfaType(userId, MfaType.valueOf(mfaType))
                .orElseThrow(()->new ResourceNotFoundException("MfaSettings","type",mfaType));
        m.setIsEnabled(false); repo.save(m);
    }
}
