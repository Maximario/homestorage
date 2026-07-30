package ru.homestorage.service;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoService {

  private final MinioClient minioClient;

  @Value("${minio.bucket-name}")
  private String bucketName;

  @Value("${minio.photos-path}")
  private String photosPath;

  @Value("${minio.thumbnail-size:300}")
  private int thumbnailSize;

  /**
   * Загрузка фото вещи
   * @return Ссылка на оригинал и превью
   */
  public PhotoUploadResult uploadPhoto(UUID itemId, MultipartFile file) {
    try {
      ensureBucketExists();

      String originalFilename = file.getOriginalFilename();
      String extension = getFileExtension(originalFilename);
      String fileName = itemId + "/original_" + UUID.randomUUID() + "." + extension;
      String thumbnailFileName = itemId + "/thumbnail_" + UUID.randomUUID() + "." + extension;

      String originalUrl = uploadFile(fileName, file.getInputStream(), file.getContentType());

      byte[] thumbnailBytes = createThumbnail(file);
      String thumbnailUrl = uploadFile(thumbnailFileName,
          new ByteArrayInputStream(thumbnailBytes),
          file.getContentType());

      log.info("Photo uploaded for item {}: original={}, thumbnail={}", itemId, originalUrl, thumbnailUrl);

      return PhotoUploadResult.builder()
          .photoUrl(originalUrl)
          .photoThumbnailUrl(thumbnailUrl)
          .build();

    } catch (Exception e) {
      log.error("Failed to upload photo for item {}", itemId, e);
      throw new RuntimeException("Failed to upload photo", e);
    }
  }

  /**
   * Загрузка файла в MinIO
   */
  private String uploadFile(String fileName, InputStream inputStream, String contentType) {
    try {
      String objectName = photosPath + fileName;

      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .stream(inputStream, -1, 10485760)
              .contentType(contentType)
              .build()
      );

      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .method(Method.GET)
              .expiry(60 * 60 * 24 * 30)
              .build()
      );

    } catch (Exception e) {
      log.error("Failed to upload file: {}", fileName, e);
      throw new RuntimeException("Failed to upload file", e);
    }
  }

  /**
   * Создание превью изображения
   */
  private byte[] createThumbnail(MultipartFile file) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      Thumbnails.of(file.getInputStream())
          .size(thumbnailSize, thumbnailSize)
          .outputFormat("jpg")
          .toOutputStream(outputStream);

      return outputStream.toByteArray();

    } catch (Exception e) {
      log.error("Failed to create thumbnail", e);
      throw new RuntimeException("Failed to create thumbnail", e);
    }
  }

  /**
   * Удаление фото вещи
   */
  public void deletePhotos(UUID itemId) {
    try {
      String prefix = photosPath + itemId + "/";

      Iterable<Result<Item>> results = minioClient.listObjects(
          ListObjectsArgs.builder()
              .bucket(bucketName)
              .prefix(prefix)
              .build()
      );

      for (Result<Item> result : results) {
        try {
          Item item = result.get();
          String objectName = item.objectName();

          minioClient.removeObject(
              RemoveObjectArgs.builder()
                  .bucket(bucketName)
                  .object(objectName)
                  .build()
          );
          log.debug("Deleted photo: {}", objectName);

        } catch (Exception e) {
          log.error("Failed to delete object: {}", e.getMessage());
        }
      }

    } catch (Exception e) {
      log.error("Failed to delete photos for item {}", itemId, e);
      throw new RuntimeException("Failed to delete photos", e);
    }
  }

  /**
   * Проверка существования bucket и создание при необходимости
   */
  private void ensureBucketExists() {
    try {
      boolean found = minioClient.bucketExists(
          BucketExistsArgs.builder().bucket(bucketName).build()
      );
      if (!found) {
        minioClient.makeBucket(
            MakeBucketArgs.builder().bucket(bucketName).build()
        );
        log.info("Bucket created: {}", bucketName);
      }
    } catch (Exception e) {
      log.error("Failed to ensure bucket exists: {}", bucketName, e);
      throw new RuntimeException("Failed to ensure bucket exists", e);
    }
  }

  private String getFileExtension(String filename) {
    if (filename == null || !filename.contains(".")) {
      return "jpg";
    }
    return filename.substring(filename.lastIndexOf(".") + 1);
  }

  @lombok.Builder
  @lombok.Data
  public static class PhotoUploadResult {
    private String photoUrl;
    private String photoThumbnailUrl;
  }
}