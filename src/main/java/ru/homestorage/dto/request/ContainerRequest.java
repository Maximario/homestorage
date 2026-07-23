package ru.homestorage.dto.request;

import ru.homestorage.model.enums.AccessLevel;
import ru.homestorage.model.enums.ContainerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerRequest {

  @NotBlank(message = "Container name is required")
  @Schema(example = "Спальня")
  private String name;

  @Schema(example = "Основная спальня на втором этаже")
  private String description;

  @NotNull(message = "Container type is required")
  private ContainerType type;

  @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID parentId;

  @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID groupId;

  @Builder.Default
  private AccessLevel accessLevel = AccessLevel.PRIVATE;
}