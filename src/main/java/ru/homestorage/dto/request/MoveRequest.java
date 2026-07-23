package ru.homestorage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveRequest {

  @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID parentId;
}