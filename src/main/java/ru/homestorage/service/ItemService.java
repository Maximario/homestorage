package ru.homestorage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homestorage.exception.AccessDeniedException;
import ru.homestorage.exception.ResourceNotFoundException;
import ru.homestorage.model.Container;
import ru.homestorage.model.Item;
import ru.homestorage.model.enums.AccessLevel;
import ru.homestorage.model.enums.ItemCategory;
import ru.homestorage.repository.ItemRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {

  private final ItemRepository itemRepository;
  private final ContainerService containerService;

  /**
   * Создание новой вещи
   */
  @Transactional
  public Item createItem(UUID userId, String name, String description,
                         ItemCategory category, UUID containerId,
                         Integer quantity, LocalDate reminderDate,
                         String reminderNote) {

    // Проверяем доступ к контейнеру (и что он существует)
    Container container = containerService.getContainerForUser(containerId, userId);

    // Проверяем права на запись
    if (!hasWriteAccess(container, userId)) {
      throw new AccessDeniedException("You don't have write access to this container");
    }

    Item item = Item.builder()
        .userId(userId)
        .name(name)
        .description(description)
        .category(category)
        .containerId(containerId)
        .quantity(quantity != null ? quantity : 1)
        .reminderDate(reminderDate)
        .reminderNote(reminderNote)
        .reminderCompleted(false)
        .build();

    item = itemRepository.save(item);
    log.info("Item created: {} (id: {}) in container: {}", name, item.getId(), containerId);
    return item;
  }

  /**
   * Получение вещи по ID с проверкой прав
   */
  public Item getItemForUser(UUID itemId, UUID userId) {
    Item item = itemRepository.findById(itemId)
        .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

    if (!item.getUserId().equals(userId)) {
      // Проверяем через контейнер (может быть групповой доступ)
      Container container = containerService.getContainerForUser(item.getContainerId(), userId);
      if (!hasReadAccess(container, userId)) {
        throw new AccessDeniedException("You don't have access to this item");
      }
    }

    return item;
  }

  /**
   * Получение всех вещей в контейнере
   */
  public List<Item> getItemsByContainer(UUID containerId, UUID userId) {
    // Проверяем доступ к контейнеру
    containerService.getContainerForUser(containerId, userId);
    return itemRepository.findByContainerId(containerId);
  }

  /**
   * Получение всех вещей пользователя (с пагинацией в будущем)
   */
  public List<Item> getAllItemsForUser(UUID userId) {
    return itemRepository.findByUserId(userId);
  }

  /**
   * Поиск вещей по названию
   */
  public List<Item> searchItems(UUID userId, String query) {
    if (query == null || query.trim().isEmpty()) {
      return itemRepository.findByUserId(userId);
    }
    return itemRepository.searchByName(userId, query.trim());
  }

  /**
   * Получение вещей по категории
   */
  public List<Item> getItemsByCategory(UUID userId, ItemCategory category) {
    return itemRepository.findByUserIdAndCategory(userId, category);
  }

  /**
   * Обновление вещи
   */
  @Transactional
  public Item updateItem(UUID itemId, UUID userId, String name,
                         String description, ItemCategory category,
                         Integer quantity, LocalDate reminderDate,
                         String reminderNote) {

    Item item = getItemForUser(itemId, userId);

    // Проверяем доступ через контейнер
    Container container = containerService.getContainerForUser(item.getContainerId(), userId);
    if (!hasWriteAccess(container, userId)) {
      throw new AccessDeniedException("You don't have write access to this item");
    }

    if (name != null) item.setName(name);
    if (description != null) item.setDescription(description);
    if (category != null) item.setCategory(category);
    if (quantity != null && quantity > 0) item.setQuantity(quantity);
    if (reminderDate != null) item.setReminderDate(reminderDate);
    if (reminderNote != null) item.setReminderNote(reminderNote);

    item = itemRepository.save(item);
    log.info("Item updated: {} (id: {})", item.getName(), itemId);
    return item;
  }

  /**
   * Перемещение вещи в другой контейнер
   */
  @Transactional
  public Item moveItem(UUID itemId, UUID newContainerId, UUID userId) {
    Item item = getItemForUser(itemId, userId);

    // Проверяем доступ к новому контейнеру
    Container newContainer = containerService.getContainerForUser(newContainerId, userId);
    if (!hasWriteAccess(newContainer, userId)) {
      throw new AccessDeniedException("You don't have write access to the target container");
    }

    // Проверяем доступ к старому контейнеру
    Container oldContainer = containerService.getContainerForUser(item.getContainerId(), userId);
    if (!hasWriteAccess(oldContainer, userId)) {
      throw new AccessDeniedException("You don't have write access to the source container");
    }

    item.setContainerId(newContainerId);
    item = itemRepository.save(item);

    log.info("Item moved: {} -> container: {}", item.getName(), newContainerId);
    return item;
  }

  /**
   * Удаление вещи
   */
  @Transactional
  public void deleteItem(UUID itemId, UUID userId) {
    Item item = getItemForUser(itemId, userId);

    Container container = containerService.getContainerForUser(item.getContainerId(), userId);
    if (!hasWriteAccess(container, userId)) {
      throw new AccessDeniedException("You don't have write access to delete this item");
    }

    itemRepository.delete(item);
    log.info("Item deleted: {} (id: {})", item.getName(), itemId);
  }

  /**
   * Отметка напоминания как выполненного
   */
  @Transactional
  public Item completeReminder(UUID itemId, UUID userId) {
    Item item = getItemForUser(itemId, userId);
    item.setReminderCompleted(true);
    item.setReminderCompletedAt(java.time.LocalDateTime.now());
    item = itemRepository.save(item);

    log.info("Reminder completed for item: {}", item.getName());
    return item;
  }

  /**
   * Получение всех активных напоминаний
   */
  public List<Item> getActiveReminders(UUID userId) {
    return itemRepository.findByUserIdAndReminderCompletedFalseOrderByReminderDateAsc(userId);
  }

  /**
   * Получение напоминаний, которые должны быть выполнены в ближайшие N дней
   */
  public List<Item> getUpcomingReminders(UUID userId, int days) {
    LocalDate date = LocalDate.now().plusDays(days);
    return itemRepository.findByUserIdAndReminderCompletedFalseAndReminderDateBefore(userId, date);
  }

  /**
   * Получение всех вещей из списка контейнеров (для группового доступа)
   */
  public List<Item> getItemsFromContainers(UUID userId, List<UUID> containerIds) {
    // Проверяем доступ к каждому контейнеру
    for (UUID containerId : containerIds) {
      containerService.getContainerForUser(containerId, userId);
    }
    return itemRepository.findByUserIdAndContainerIdIn(userId, containerIds);
  }

  /**
   * Статистика: количество вещей по категориям
   */
  public long countItemsByCategory(UUID userId, ItemCategory category) {
    return itemRepository.countByUserIdAndCategory(userId, category);
  }

  /**
   * Статистика: общее количество вещей
   */
  public long countItemsForUser(UUID userId) {
    return itemRepository.countByUserId(userId);
  }

  // === Вспомогательные методы ===

  private boolean hasReadAccess(Container container, UUID userId) {
    // Проверяем через ContainerService
    try {
      containerService.getContainerForUser(container.getId(), userId);
      return true;
    } catch (AccessDeniedException e) {
      return false;
    }
  }

  private boolean hasWriteAccess(Container container, UUID userId) {
    // Проверяем, имеет ли пользователь права на запись
    // Здесь может быть своя логика или вызов метода из ContainerService
    // Пока используем упрощённую проверку
    if (container.getAccessLevel() == AccessLevel.PRIVATE) {
      return container.getUserId().equals(userId);
    }
    if (container.getAccessLevel() == AccessLevel.GROUP_WRITE) {
      // Проверяем членство в группе (через GroupMemberRepository)
      return true; // Заглушка, нужно добавить проверку через GroupMemberRepository
    }
    return false;
  }
}