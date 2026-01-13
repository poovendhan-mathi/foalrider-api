package com.foalrider.modules.upload.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Cloudinary implementation of ImageUploadService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryImageUploadService implements ImageUploadService {

    private final Cloudinary cloudinary;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    public Map<String, Object> uploadImage(MultipartFile file, String folder) {
        validateFile(file);

        try {
            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "unique_filename", true,
                    "overwrite", false,
                    "transformation", new Transformation<>()
                            .quality("auto")
                            .fetchFormat("auto")
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);

            log.info("Successfully uploaded image to Cloudinary: {}", uploadResult.get("public_id"));

            return Map.of(
                    "url", uploadResult.get("secure_url"),
                    "publicId", uploadResult.get("public_id"),
                    "format", uploadResult.get("format"),
                    "width", uploadResult.get("width"),
                    "height", uploadResult.get("height"),
                    "bytes", uploadResult.get("bytes")
            );

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    @Override
    public List<Map<String, Object>> uploadImages(List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                results.add(uploadImage(file, folder));
            } catch (Exception e) {
                log.error("Failed to upload file {}: {}", file.getOriginalFilename(), e.getMessage());
                // Continue with other files
            }
        }
        return results;
    }

    @Override
    public boolean deleteImage(String publicId) {
        if (publicId == null || publicId.isEmpty()) {
            return false;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String status = (String) result.get("result");
            boolean success = "ok".equals(status);

            if (success) {
                log.info("Successfully deleted image from Cloudinary: {}", publicId);
            } else {
                log.warn("Failed to delete image from Cloudinary: {} - Status: {}", publicId, status);
            }

            return success;

        } catch (IOException e) {
            log.error("Error deleting image from Cloudinary: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int deleteImages(List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (String publicId : publicIds) {
            if (deleteImage(publicId)) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    public String getTransformedUrl(String publicId, int width, int height) {
        if (publicId == null || publicId.isEmpty()) {
            return null;
        }

        return cloudinary.url()
                .transformation(new Transformation<>()
                        .width(width)
                        .height(height)
                        .crop("fill")
                        .gravity("auto")
                        .quality("auto")
                        .fetchFormat("auto"))
                .secure(true)
                .generate(publicId);
    }

    @Override
    public String getThumbnailUrl(String publicId) {
        return getTransformedUrl(publicId, 150, 150);
    }

    /**
     * Generate a URL optimized for product display.
     */
    public String getProductImageUrl(String publicId, int width) {
        if (publicId == null || publicId.isEmpty()) {
            return null;
        }

        return cloudinary.url()
                .transformation(new Transformation<>()
                        .width(width)
                        .crop("scale")
                        .quality("auto:good")
                        .fetchFormat("auto"))
                .secure(true)
                .generate(publicId);
    }

    /**
     * Generate a URL for product gallery (square crop).
     */
    public String getProductGalleryUrl(String publicId) {
        if (publicId == null || publicId.isEmpty()) {
            return null;
        }

        return cloudinary.url()
                .transformation(new Transformation<>()
                        .width(800)
                        .height(800)
                        .crop("fill")
                        .gravity("auto")
                        .quality("auto")
                        .fetchFormat("auto"))
                .secure(true)
                .generate(publicId);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
        }
    }
}
