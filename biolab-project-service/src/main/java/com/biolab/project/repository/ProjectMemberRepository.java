package com.biolab.project.repository;

import com.biolab.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    List<ProjectMember> findByProjectId(UUID projectId);
    List<ProjectMember> findByUserId(UUID userId);
    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);
}
