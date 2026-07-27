package ru.homestorage.dto.request;

import ru.homestorage.model.enums.ItemCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequest {

  @NotBlank(message = "Item name is required")
  @Schema(example = "Зимняя куртка")
  private String name;

  @Schema(example = "Пуховик, синий, размер L")
  private String description;

  @NotNull(message = "Category is required")
  private ItemCategory category;

  @NotNull(message = "Container ID is required")
  @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID containerId;

  @Min(value = 1, message = "Quantity must be at least 1")
  @Schema(example = "2")
  private Integer quantity;

  @Schema(example = "2026-12-31")
  private LocalDate reminderDate;

  @Schema(example = "Проверить срок годности")
  private String reminderNote;
}