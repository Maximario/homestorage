package ru.homestorage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.homestorage.model.GroupMember;
import ru.homestorage.model.enums.GroupRole;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

  boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

  @Query("SELECT gm.userId FROM GroupMember gm WHERE gm.groupId = :groupId")
  List<UUID> findUserIdsByGroupId(@Param("groupId") UUID groupId);

  @Query("SELECT gm.groupId FROM GroupMember gm WHERE gm.userId = :userId")
  List<UUID> findGroupIdsByUserId(@Param("userId") UUID userId);

  List<GroupMember> findByGroupId(UUID groupId);

  boolean existsByGroupIdAndUserIdAndRole(UUID groupId, UUID userId, GroupRole role);
}