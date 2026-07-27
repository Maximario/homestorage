package ru.homestorage.dto.response;

import ru.homestorage.model.Item;
import ru.homestorage.model.enums.ItemCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponse {
  private UUID id;
  private String name;
  private String description;
  private ItemCategory category;
  private UUID containerId;
  private Integer quantity;
  private String photoUrl;
  private String photoThumbnailUrl;
  private LocalDate reminderDate;
  private String reminderNote;
  private Boolean reminderCompleted;
  private LocalDateTime reminderCompletedAt;
  private LocalDateTime addedAt;
  private LocalDateTime updatedAt;

  public static ItemResponse fromEntity(Item item) {
    return ItemResponse.builder()
        .id(item.getId())
        .name(item.getName())
        .description(item.getDescription())
        .category(item.getCategory())
        .containerId(item.getContainerId())
        .quantity(item.getQuantity())
        .photoUrl(item.getPhotoUrl())
        .photoThumbnailUrl(item.getPhotoThumbnailUrl())
        .reminderDate(item.getReminderDate())
        .reminderNote(item.getReminderNote())
        .reminderCompleted(item.getReminderCompleted())
        .reminderCompletedAt(item.getReminderCompletedAt())
        .addedAt(item.getAddedAt())
        .updatedAt(item.getUpdatedAt())
        .build();
  }
}