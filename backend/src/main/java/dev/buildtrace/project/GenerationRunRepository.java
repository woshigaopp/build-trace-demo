package dev.buildtrace.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface GenerationRunRepository extends JpaRepository<GenerationRunEntity, String> {

    List<GenerationRunEntity> findAllByProjectIdOrderByCreatedAtAsc(String projectId);

    Optional<GenerationRunEntity> findFirstByProjectIdAndStatusIn(String projectId, Collection<String> statuses);

    List<GenerationRunEntity> findAllByStatusIn(Collection<String> statuses);

    Optional<GenerationRunEntity> findByIdAndProjectId(String id, String projectId);
}
