package com.biolab.audit.repository;
import com.biolab.audit.entity.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, UUID> {
    Optional<PlatformSetting> findByKey(String key);
    List<PlatformSetting> findByCategory(String category);
}
