package ru.homestorage.model;

import ru.homestorage.model.enums.ItemCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ItemCategory category;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "container_id", nullable = false)
  private UUID containerId;

  @Builder.Default
  private Integer quantity = 1;

  @Column(name = "photo_url")
  private String photoUrl;

  @Column(name = "photo_thumbnail_url")
  private String photoThumbnailUrl;

  @Column(name = "reminder_date")
  private LocalDate reminderDate;

  @Column(name = "reminder_note")
  private String reminderNote;

  @Column(name = "reminder_completed")
  @Builder.Default
  private Boolean reminderCompleted = false;

  @Column(name = "reminder_completed_at")
  private LocalDateTime reminderCompletedAt;

  @CreationTimestamp
  @Column(name = "added_at", updatable = false)
  private LocalDateTime addedAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}