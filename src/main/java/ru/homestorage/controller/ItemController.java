package ru.homestorage.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.homestorage.dto.request.ItemRequest;
import ru.homestorage.dto.request.MoveItemRequest;
import ru.homestorage.dto.response.ItemResponse;
import ru.homestorage.exception.BadRequestException;
import ru.homestorage.model.Item;
import ru.homestorage.model.enums.ItemCategory;
import ru.homestorage.service.CustomUserDetails;
import ru.homestorage.service.ItemService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

  private final ItemService itemService;

  @GetMapping
  public ResponseEntity<List<ItemResponse>> getAllItems(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    List<Item> items = itemService.getAllItemsForUser(userId);
    return ResponseEntity.ok(items.stream()
        .map(ItemResponse::fromEntity)
        .collect(Collectors.toList()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ItemResponse> getItem(
      @PathVariable UUID id,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    Item item = itemService.getItemForUser(id, userId);
    return ResponseEntity.ok(ItemResponse.fromEntity(item));
  }

  @GetMapping("/container/{containerId}")
  public ResponseEntity<List<ItemResponse>> getItemsByContainer(
      @PathVariable UUID containerId,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    List<Item> items = itemService.getItemsByContainer(containerId, userId);
    return ResponseEntity.ok(items.stream()
        .map(ItemResponse::fromEntity)
        .collect(Collectors.toList()));
  }

  @GetMapping("/search")
  public ResponseEntity<List<ItemResponse>> searchItems(
      @RequestParam String query,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    List<Item> items = itemService.searchItems(userId, query);
    return ResponseEntity.ok(items.stream()
        .map(ItemResponse::fromEntity)
        .collect(Collectors.toList()));
  }

  @GetMapping("/category/{category}")
  public ResponseEntity<List<ItemResponse>> getItemsByCategory(
      @PathVariable ItemCategory category,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    List<Item> items = itemService.getItemsByCategory(userId, category);
    return ResponseEntity.ok(items.stream()
        .map(ItemResponse::fromEntity)
        .collect(Collectors.toList()));
  }

  @GetMapping("/reminders/active")
  public ResponseEntity<List<ItemResponse>> getActiveReminders(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    List<Item> items = itemService.getActiveReminders(userId);
    return ResponseEntity.ok(items.stream()
        .map(ItemResponse::fromEntity)
        .collect(Collectors.toList()));
  }

  @GetMapping("/reminders/upcoming")
  public ResponseEntity<List<ItemResponse>> getUpcomingReminders(
      @RequestParam(defaultValue = "7") int days,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    List<Item> items = itemService.getUpcomingReminders(userId, days);
    return ResponseEntity.ok(items.stream()
        .map(ItemResponse::fromEntity)
        .collect(Collectors.toList()));
  }

  @PostMapping
  public ResponseEntity<ItemResponse> createItem(
      @Valid @RequestBody ItemRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    Item item = itemService.createItem(
        userId,
        request.getName(),
        request.getDescription(),
        request.getCategory(),
        request.getContainerId(),
        request.getQuantity(),
        request.getReminderDate(),
        request.getReminderNote()
    );
    return ResponseEntity.ok(ItemResponse.fromEntity(item));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ItemResponse> updateItem(
      @PathVariable UUID id,
      @Valid @RequestBody ItemRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    Item item = itemService.updateItem(
        id,
        userId,
        request.getName(),
        request.getDescription(),
        request.getCategory(),
        request.getQuantity(),
        request.getReminderDate(),
        request.getReminderNote()
    );
    return ResponseEntity.ok(ItemResponse.fromEntity(item));
  }

  @PatchMapping("/{id}/move")
  public ResponseEntity<ItemResponse> moveItem(
      @PathVariable UUID id,
      @Valid @RequestBody MoveItemRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    Item item = itemService.moveItem(id, request.getContainerId(), userId);
    return ResponseEntity.ok(ItemResponse.fromEntity(item));
  }

  @PatchMapping("/{id}/reminder/complete")
  public ResponseEntity<ItemResponse> completeReminder(
      @PathVariable UUID id,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    Item item = itemService.completeReminder(id, userId);
    return ResponseEntity.ok(ItemResponse.fromEntity(item));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteItem(
      @PathVariable UUID id,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    itemService.deleteItem(id, userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ItemResponse> uploadPhoto(
      @PathVariable UUID id,
      @RequestParam("file") MultipartFile file,
      @AuthenticationPrincipal CustomUserDetails userDetails) {

    // Проверка размера файла
    if (file.getSize() > 10 * 1024 * 1024) { // 10MB
      throw new BadRequestException("File size exceeds 10MB limit");
    }

    // Проверка типа файла
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new BadRequestException("Only image files are allowed");
    }

    UUID userId = userDetails.getUserId();
    Item item = itemService.addPhoto(id, userId, file);
    return ResponseEntity.ok(ItemResponse.fromEntity(item));
  }

  @DeleteMapping("/{id}/photo")
  public ResponseEntity<Void> deletePhoto(
      @PathVariable UUID id,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID userId = userDetails.getUserId();
    itemService.deletePhoto(id, userId);
    return ResponseEntity.noContent().build();
  }
}