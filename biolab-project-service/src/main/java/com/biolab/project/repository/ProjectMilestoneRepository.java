package com.biolab.project.repository;

import com.biolab.project.entity.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, UUID> {
    List<ProjectMilestone> findByProjectIdOrderBySortOrder(UUID projectId);
}
