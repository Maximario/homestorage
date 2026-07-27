package ru.homestorage.repository;

import ru.homestorage.model.Item;
import ru.homestorage.model.enums.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

  List<Item> findByContainerId(UUID containerId);

  List<Item> findByUserId(UUID userId);

  List<Item> findByContainerIdAndUserId(UUID containerId, UUID userId);

  List<Item> findByUserIdAndCategory(UUID userId, ItemCategory category);

  List<Item> findByUserIdAndReminderCompletedFalseOrderByReminderDateAsc(UUID userId);

  List<Item> findByUserIdAndReminderCompletedFalseAndReminderDateBefore(UUID userId, LocalDate date);

  @Query("SELECT i FROM Item i WHERE i.userId = :userId AND LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%'))")
  List<Item> searchByName(@Param("userId") UUID userId, @Param("query") String query);

  @Query("SELECT i FROM Item i WHERE i.userId = :userId AND i.containerId IN :containerIds")
  List<Item> findByUserIdAndContainerIdIn(@Param("userId") UUID userId, @Param("containerIds") List<UUID> containerIds);

  long countByContainerId(UUID containerId);

  long countByUserId(UUID userId);

  long countByUserIdAndCategory(UUID userId, ItemCategory category);
}