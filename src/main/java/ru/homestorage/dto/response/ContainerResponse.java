package ru.homestorage.dto.response;

import ru.homestorage.model.Container;
import ru.homestorage.model.enums.AccessLevel;
import ru.homestorage.model.enums.ContainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerResponse {
  private UUID id;
  private String name;
  private String description;
  private ContainerType type;
  private UUID parentId;
  private UUID groupId;
  private AccessLevel accessLevel;
  private String qrCode;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static ContainerResponse fromEntity(Container container) {
    return ContainerResponse.builder()
        .id(container.getId())
        .name(container.getName())
        .description(container.getDescription())
        .type(container.getType())
        .parentId(container.getParentId())
        .groupId(container.getGroupId())
        .accessLevel(container.getAccessLevel())
        .qrCode(container.getQrCode())
        .createdAt(container.getCreatedAt())
        .updatedAt(container.getUpdatedAt())
        .build();
  }
}