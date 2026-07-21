package ru.homestorage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.homestorage.model.enums.AccessLevel;
import ru.homestorage.model.enums.ContainerType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "containers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Container {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "group_id")
  private UUID groupId;

  @Enumerated(EnumType.STRING)
  @Column(name = "access_level", nullable = false)
  @Builder.Default
  private AccessLevel accessLevel = AccessLevel.PRIVATE;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "parent_id")
  private UUID parentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContainerType type;

  @Column(name = "qr_code", columnDefinition = "TEXT")
  private String qrCode;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
