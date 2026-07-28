package dev.buildtrace.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ProjectRepository extends JpaRepository<ProjectEntity, String> {

    List<ProjectEntity> findAllByGuestIdOrderByUpdatedAtDesc(String guestId);

    Optional<ProjectEntity> findByIdAndGuestId(String id, String guestId);
}
