package ru.homestorage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.homestorage.model.Container;
import ru.homestorage.model.enums.AccessLevel;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContainerRepository extends JpaRepository<Container, UUID> {

  List<Container> findByUserIdAndParentIdIsNullAndAccessLevel(UUID userId, AccessLevel accessLevel);

  List<Container> findByGroupIdInAndParentIdIsNullAndAccessLevelIn(
      List<UUID> groupIds,
      List<AccessLevel> accessLevels
  );

  List<Container> findByParentId(UUID parentId);

  @Query("SELECT c FROM Container c WHERE c.parentId = :parentId AND c.userId = :userId")
  List<Container> findByParentIdAndUserId(@Param("parentId") UUID parentId, @Param("userId") UUID userId);

  @Query("SELECT c FROM Container c WHERE c.parentId = :parentId AND c.groupId IN :groupIds")
  List<Container> findByParentIdAndGroupIdIn(
      @Param("parentId") UUID parentId,
      @Param("groupIds") List<UUID> groupIds
  );

  @Query(value = """
        WITH RECURSIVE descendants AS (
            SELECT id, parent_id FROM containers WHERE id = :containerId
            UNION ALL
            SELECT c.id, c.parent_id FROM containers c
            INNER JOIN descendants d ON d.id = c.parent_id
        )
        SELECT * FROM containers WHERE id IN (SELECT id FROM descendants) AND id != :containerId
        """, nativeQuery = true)
  List<Container> findAllDescendants(@Param("containerId") UUID containerId);
}