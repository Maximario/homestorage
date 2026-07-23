package ru.homestorage.service;

import ru.homestorage.exception.AccessDeniedException;
import ru.homestorage.exception.ResourceNotFoundException;
import ru.homestorage.model.Container;
import ru.homestorage.model.enums.AccessLevel;
import ru.homestorage.model.enums.ContainerType;
import ru.homestorage.repository.ContainerRepository;
import ru.homestorage.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerService {

  private final ContainerRepository containerRepository;
  private final GroupMemberRepository groupMemberRepository;

  /**
   * Валидация иерархии: проверяет, может ли родитель содержать дочерний контейнер
   */
  private void validateParentType(ContainerType parentType, ContainerType childType) {
    // BUILDING → только ROOM
    if (parentType == ContainerType.BUILDING && childType != ContainerType.ROOM) {
      throw new IllegalArgumentException("Building can only contain rooms");
    }

    // ROOM → FURNITURE, SHELF, BOX, DRAWER
    if (parentType == ContainerType.ROOM &&
        childType != ContainerType.FURNITURE &&
        childType != ContainerType.SHELF &&
        childType != ContainerType.BOX &&
        childType != ContainerType.DRAWER) {
      throw new IllegalArgumentException("Room can only contain furniture, shelves, boxes, or drawers");
    }

    // FURNITURE → SHELF, BOX, DRAWER
    if (parentType == ContainerType.FURNITURE &&
        childType != ContainerType.SHELF &&
        childType != ContainerType.BOX &&
        childType != ContainerType.DRAWER) {
      throw new IllegalArgumentException("Furniture can only contain shelves, boxes, or drawers");
    }

    // SHELF → BOX, DRAWER
    if (parentType == ContainerType.SHELF &&
        childType != ContainerType.BOX &&
        childType != ContainerType.DRAWER) {
      throw new IllegalArgumentException("Shelf can only contain boxes or drawers");
    }

    // BOX → НИЧЕГО (только вещи)
    if (parentType == ContainerType.BOX) {
      throw new IllegalArgumentException("Box cannot contain other containers");
    }

    // DRAWER → может содержать только BOX
    if (parentType == ContainerType.DRAWER && childType != ContainerType.BOX) {
      throw new IllegalArgumentException("Drawer can only contain boxes");
    }
  }

  /**
   * Проверка доступа к контейнеру
   */
  private boolean hasAccess(Container container, UUID userId, AccessLevel requiredAccess) {
    // Если контейнер приватный
    if (container.getAccessLevel() == AccessLevel.PRIVATE) {
      return container.getUserId().equals(userId);
    }

    // Если контейнер групповой
    if (container.getGroupId() != null) {
      boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(container.getGroupId(), userId);
      if (!isMember) {
        return false;
      }

      // Если требуется запись - проверяем, что доступ уровня WRITE
      if (requiredAccess == AccessLevel.GROUP_WRITE) {
        return container.getAccessLevel() == AccessLevel.GROUP_WRITE;
      }

      // Если требуется чтение - подходит любой групповой доступ
      return container.getAccessLevel() == AccessLevel.GROUP_READ ||
          container.getAccessLevel() == AccessLevel.GROUP_WRITE;
    }

    return false;
  }

  /**
   * Проверка, является ли один контейнер потомком другого
   */
  private boolean isDescendant(UUID ancestorId, UUID descendantId) {
    UUID currentId = descendantId;
    while (currentId != null) {
      if (currentId.equals(ancestorId)) {
        return true;
      }
      Container parent = containerRepository.findById(currentId).orElse(null);
      if (parent == null) {
        break;
      }
      currentId = parent.getParentId();
    }
    return false;
  }

  /**
   * Получение контейнера с проверкой прав доступа
   */
  public Container getContainerForUser(UUID containerId, UUID userId) {
    Container container = containerRepository.findById(containerId)
        .orElseThrow(() -> new ResourceNotFoundException("Container not found with id: " + containerId));

    if (!hasAccess(container, userId, AccessLevel.GROUP_READ)) {
      throw new AccessDeniedException("You don't have access to this container");
    }

    return container;
  }

  /**
   * Получение всех корневых контейнеров пользователя
   */
  public List<Container> getRootContainers(UUID userId) {
    // Получаем приватные контейнеры пользователя
    List<Container> privateRoots = containerRepository
        .findByUserIdAndParentIdIsNullAndAccessLevel(userId, AccessLevel.PRIVATE);

    // Получаем групповые контейнеры, где пользователь является участником
    List<UUID> groupIds = groupMemberRepository.findGroupIdsByUserId(userId);
    List<Container> groupRoots = containerRepository
        .findByGroupIdInAndParentIdIsNullAndAccessLevelIn(
            groupIds,
            List.of(AccessLevel.GROUP_READ, AccessLevel.GROUP_WRITE)
        );

    List<Container> allRoots = new ArrayList<>();
    allRoots.addAll(privateRoots);
    allRoots.addAll(groupRoots);

    return allRoots;
  }

  /**
   * Получение всех дочерних контейнеров
   */
  public List<Container> getChildContainers(UUID parentId, UUID userId) {
    // Проверяем доступ к родителю
    getContainerForUser(parentId, userId);

    return containerRepository.findByParentId(parentId);
  }

  /**
   * Создание нового контейнера
   */
  @Transactional
  public Container createContainer(UUID userId, String name, String description,
                                   ContainerType type, UUID parentId,
                                   UUID groupId, AccessLevel accessLevel) {
    // Проверяем родительский контейнер
    if (parentId != null) {
      Container parent = getContainerForUser(parentId, userId);

      // Проверяем валидность иерархии
      validateParentType(parent.getType(), type);

      // Нельзя добавлять контейнеры в коробку или ящик (доп. проверка)
      if (parent.getType() == ContainerType.BOX || parent.getType() == ContainerType.DRAWER) {
        throw new IllegalArgumentException("Cannot add container inside a box or drawer.");
      }
    }

    // Если контейнер групповой - проверяем, что пользователь является участником группы
    if (groupId != null) {
      boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
      if (!isMember) {
        throw new AccessDeniedException("User is not a member of this group");
      }
    }

    Container container = Container.builder()
        .userId(userId)
        .name(name)
        .description(description)
        .type(type)
        .parentId(parentId)
        .groupId(groupId)
        .accessLevel(accessLevel != null ? accessLevel : AccessLevel.PRIVATE)
        .build();

    container = containerRepository.save(container);

    log.info("Container created: {} (id: {}, type: {})", name, container.getId(), type);
    return container;
  }

  /**
   * Обновление контейнера
   */
  @Transactional
  public Container updateContainer(UUID containerId, UUID userId, String name,
                                   String description, ContainerType type,
                                   AccessLevel accessLevel, UUID groupId) {
    Container container = getContainerForUser(containerId, userId);

    // Проверяем права на запись
    if (!hasAccess(container, userId, AccessLevel.GROUP_WRITE)) {
      throw new AccessDeniedException("You don't have write access to this container");
    }

    // Если меняется группа - проверяем, что пользователь участник новой группы
    if (groupId != null && !groupId.equals(container.getGroupId())) {
      boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
      if (!isMember) {
        throw new AccessDeniedException("User is not a member of the new group");
      }
    }

    if (name != null) container.setName(name);
    if (description != null) container.setDescription(description);
    if (type != null) container.setType(type);
    if (accessLevel != null) container.setAccessLevel(accessLevel);
    if (groupId != null) container.setGroupId(groupId);

    container = containerRepository.save(container);

    log.info("Container updated: {}", containerId);
    return container;
  }

  /**
   * Перемещение контейнера в другой родительский контейнер
   */
  @Transactional
  public Container moveContainer(UUID containerId, UUID newParentId, UUID userId) {
    Container container = getContainerForUser(containerId, userId);

    // Проверяем права на запись
    if (!hasAccess(container, userId, AccessLevel.GROUP_WRITE)) {
      throw new AccessDeniedException("You don't have write access to this container");
    }

    // Если новый родитель указан - проверяем доступ к нему
    if (newParentId != null) {
      Container newParent = getContainerForUser(newParentId, userId);

      // Проверяем, что не пытаемся переместить контейнер в самого себя
      if (newParentId.equals(containerId)) {
        throw new IllegalArgumentException("Cannot move container into itself");
      }

      // Проверяем валидность иерархии
      validateParentType(newParent.getType(), container.getType());

      // Проверяем, что новый родитель не является потомком текущего контейнера (защита от циклов)
      if (isDescendant(containerId, newParentId)) {
        throw new IllegalArgumentException("Cannot move container into its descendant");
      }

      // Нельзя переместить в коробку или ящик
      if (newParent.getType() == ContainerType.BOX || newParent.getType() == ContainerType.DRAWER) {
        throw new IllegalArgumentException("Cannot move container into a box or drawer");
      }
    }

    container.setParentId(newParentId);
    container = containerRepository.save(container);

    log.info("Container moved: {} -> new parent: {}", containerId, newParentId);
    return container;
  }

  /**
   * Удаление контейнера (каскадно)
   */
  @Transactional
  public void deleteContainer(UUID containerId, UUID userId) {
    Container container = getContainerForUser(containerId, userId);

    // Проверяем права на запись
    if (!hasAccess(container, userId, AccessLevel.GROUP_WRITE)) {
      throw new AccessDeniedException("You don't have write access to this container");
    }

    // Получаем все дочерние контейнеры
    List<Container> descendants = containerRepository.findAllDescendants(containerId);
    for (Container desc : descendants) {
      if (!hasAccess(desc, userId, AccessLevel.GROUP_WRITE)) {
        throw new AccessDeniedException("You don't have write access to descendant container: " + desc.getId());
      }
    }

    containerRepository.delete(container);

    log.info("Container deleted: {} (id: {})", container.getName(), containerId);
  }

  /**
   * Получение пути к контейнеру (например, "Спальня → Шкаф → Коробка №5")
   */
  public String getContainerPath(UUID containerId, UUID userId) {
    Container container = getContainerForUser(containerId, userId);
    return buildPath(container, userId);
  }

  private String buildPath(Container container, UUID userId) {
    if (container.getParentId() == null) {
      return container.getName();
    }

    Container parent = containerRepository.findById(container.getParentId())
        .orElseThrow(() -> new ResourceNotFoundException("Parent container not found"));

    // Проверяем доступ к родителю
    if (!hasAccess(parent, userId, AccessLevel.GROUP_READ)) {
      return container.getName(); // Если нет доступа к родителю - показываем только текущий контейнер
    }

    return buildPath(parent, userId) + " → " + container.getName();
  }

  /**
   * Получение полного дерева контейнеров (рекурсивно)
   */
  public Map<String, Object> getContainerTree(UUID containerId, UUID userId) {
    Container root = getContainerForUser(containerId, userId);
    return buildTree(root, userId);
  }

  private Map<String, Object> buildTree(Container container, UUID userId) {
    Map<String, Object> node = new LinkedHashMap<>();
    node.put("id", container.getId());
    node.put("name", container.getName());
    node.put("type", container.getType());
    node.put("accessLevel", container.getAccessLevel());
    node.put("groupId", container.getGroupId());
    node.put("createdAt", container.getCreatedAt());

    // Получаем дочерние контейнеры
    List<Container> children = containerRepository.findByParentId(container.getId());

    // Фильтруем только те, к которым есть доступ
    List<Map<String, Object>> childNodes = children.stream()
        .filter(child -> hasAccess(child, userId, AccessLevel.GROUP_READ))
        .map(child -> buildTree(child, userId))
        .collect(Collectors.toList());

    node.put("children", childNodes);
    node.put("itemsCount", childNodes.size());

    return node;
  }
}