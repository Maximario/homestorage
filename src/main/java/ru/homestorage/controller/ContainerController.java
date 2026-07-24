package ru.homestorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.homestorage.dto.request.ContainerRequest;
import ru.homestorage.dto.request.MoveRequest;
import ru.homestorage.dto.response.ContainerResponse;
import ru.homestorage.model.Container;
import ru.homestorage.service.ContainerService;
import ru.homestorage.service.CustomUserDetails;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/containers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-auth")
public class ContainerController {

  private final ContainerService containerService;

  @Operation(summary = "Get root containers for current user")
  @GetMapping
  public ResponseEntity<List<ContainerResponse>> getRootContainers(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    List<Container> containers = containerService.getRootContainers(userId);
    return ResponseEntity.ok(containers.stream()
        .map(ContainerResponse::fromEntity)
        .collect(Collectors.toList()));
  }

  @Operation(summary = "Get container by ID")
  @GetMapping("/{id}")
  public ResponseEntity<ContainerResponse> getContainer(
      @PathVariable UUID id,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    Container container = containerService.getContainerForUser(id, userId);
    return ResponseEntity.ok(ContainerResponse.fromEntity(container));
  }

  @Operation(summary = "Get child containers")
  @GetMapping("/{id}/children")
  public ResponseEntity<List<ContainerResponse>> getChildContainers(
      @PathVariable UUID id,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    List<Container> children = containerService.getChildContainers(id, userId);
    return ResponseEntity.ok(children.stream()
        .map(ContainerResponse::fromEntity)
        .collect(Collectors.toList()));
  }

  @Operation(summary = "Get full container tree")
  @GetMapping("/{id}/tree")
  public ResponseEntity<Map<String, Object>> getContainerTree(
      @PathVariable UUID id,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    Map<String, Object> tree = containerService.getContainerTree(id, userId);
    return ResponseEntity.ok(tree);
  }

  @Operation(summary = "Get container path")
  @GetMapping("/{id}/path")
  public ResponseEntity<String> getContainerPath(
      @PathVariable UUID id,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    String path = containerService.getContainerPath(id, userId);
    return ResponseEntity.ok(path);
  }

  @Operation(summary = "Create new container")
  @PostMapping
  public ResponseEntity<ContainerResponse> createContainer(
      @Valid @RequestBody ContainerRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {  // <-- CustomUserDetails
    UUID userId = userDetails.getUserId();
    Container container = containerService.createContainer(
        userId,
        request.getName(),
        request.getDescription(),
        request.getType(),
        request.getParentId(),
        request.getGroupId(),
        request.getAccessLevel()
    );
    return ResponseEntity.ok(ContainerResponse.fromEntity(container));
  }

  @Operation(summary = "Update container")
  @PutMapping("/{id}")
  public ResponseEntity<ContainerResponse> updateContainer(
      @PathVariable UUID id,
      @Valid @RequestBody ContainerRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    Container container = containerService.updateContainer(
        id,
        userId,
        request.getName(),
        request.getDescription(),
        request.getType(),
        request.getAccessLevel(),
        request.getGroupId()
    );
    return ResponseEntity.ok(ContainerResponse.fromEntity(container));
  }

  @Operation(summary = "Move container to new parent")
  @PatchMapping("/{id}/move")
  public ResponseEntity<ContainerResponse> moveContainer(
      @PathVariable UUID id,
      @Valid @RequestBody MoveRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    Container container = containerService.moveContainer(id, request.getParentId(), userId);
    return ResponseEntity.ok(ContainerResponse.fromEntity(container));
  }

  @Operation(summary = "Delete container")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteContainer(
      @PathVariable UUID id,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    containerService.deleteContainer(id, userId);
    return ResponseEntity.noContent().build();
  }
}