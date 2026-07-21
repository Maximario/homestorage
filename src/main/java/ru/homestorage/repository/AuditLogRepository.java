package ru.homestorage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.homestorage.model.AuditLog;

import java.util.List;
import java.util.UUID;

@Repository

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
  List<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
  List<AuditLog> findByEntityIdOrderByCreatedAtDesc(UUID entityId);
}