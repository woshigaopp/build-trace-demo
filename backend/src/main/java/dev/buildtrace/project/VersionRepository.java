package dev.buildtrace.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface VersionRepository extends JpaRepository<VersionEntity, String> {

    List<VersionEntity> findAllByProjectIdOrderByVersionNumberDesc(String projectId);

    Optional<VersionEntity> findFirstByProjectIdOrderByVersionNumberDesc(String projectId);

    Optional<VersionEntity> findByIdAndProjectId(String id, String projectId);
}
