package dev.buildtrace.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface MessageRepository extends JpaRepository<MessageEntity, String> {

    List<MessageEntity> findAllByProjectIdOrderByCreatedAtAsc(String projectId);
}
