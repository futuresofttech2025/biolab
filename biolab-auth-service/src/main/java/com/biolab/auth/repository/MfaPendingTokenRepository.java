package com.biolab.auth.repository;

import com.biolab.auth.entity.MfaPendingToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Redis CRUD repository for {@link MfaPendingToken}.
 *
 * <p>Spring Data Redis handles TTL-based eviction automatically — no
 * scheduled cleanup needed. Entries disappear exactly 5 minutes after
 * they are created.</p>
 *
 * @author BioLab Engineering Team
 */
@Repository
public interface MfaPendingTokenRepository extends CrudRepository<MfaPendingToken, String> {
}
