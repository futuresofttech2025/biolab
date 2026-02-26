package com.biolab.notification.repository;

import com.biolab.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    Optional<NotificationPreference> findByUserId(UUID userId);
}
