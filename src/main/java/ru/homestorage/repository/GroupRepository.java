package ru.homestorage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.homestorage.model.Group;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
  List<Group> findByCreatedBy(UUID userId);
  boolean existsByIdAndCreatedBy(UUID groupId, UUID userId);
}