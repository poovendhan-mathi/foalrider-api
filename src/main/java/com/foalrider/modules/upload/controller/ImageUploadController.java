package com.foalrider.modules.upload.controller;

import com.foalrider.modules.upload.dto.ImageUploadResponse;
import com.foalrider.modules.upload.service.ImageUploadService;
import com.foalrider.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controller for image upload operations.
 */
@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Tag(name = "Uploads", description = "Image upload endpoints")
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload a product image")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadProductImage(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = imageUploadService.uploadImage(file, "products");
        ImageUploadResponse response = mapToResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response, "Image uploaded successfully"));
    }

    @PostMapping(value = "/products/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload multiple product images")
    public ResponseEntity<ApiResponse<List<ImageUploadResponse>>> uploadProductImages(
            @RequestParam("files") List<MultipartFile> files) {
        List<Map<String, Object>> results = imageUploadService.uploadImages(files, "products");
        List<ImageUploadResponse> responses = results.stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses, "Images uploaded successfully"));
    }

    @PostMapping(value = "/categories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload a category image")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadCategoryImage(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = imageUploadService.uploadImage(file, "categories");
        ImageUploadResponse response = mapToResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response, "Image uploaded successfully"));
    }

    @PostMapping(value = "/brands", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload a brand logo")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadBrandImage(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = imageUploadService.uploadImage(file, "brands");
        ImageUploadResponse response = mapToResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response, "Image uploaded successfully"));
    }

    @PostMapping(value = "/users/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload user avatar")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadUserAvatar(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = imageUploadService.uploadImage(file, "avatars");
        ImageUploadResponse response = mapToResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response, "Avatar uploaded successfully"));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an image")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable String publicId) {
        boolean deleted = imageUploadService.deleteImage(publicId);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success(null, "Image deleted successfully"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete image"));
        }
    }

    @GetMapping("/transform")
    @Operation(summary = "Get transformed image URL")
    public ResponseEntity<ApiResponse<String>> getTransformedUrl(
            @RequestParam String publicId,
            @RequestParam(defaultValue = "800") int width,
            @RequestParam(defaultValue = "800") int height) {
        String url = imageUploadService.getTransformedUrl(publicId, width, height);
        return ResponseEntity.ok(ApiResponse.success(url, "URL generated successfully"));
    }

    @GetMapping("/thumbnail")
    @Operation(summary = "Get thumbnail URL")
    public ResponseEntity<ApiResponse<String>> getThumbnailUrl(@RequestParam String publicId) {
        String url = imageUploadService.getThumbnailUrl(publicId);
        return ResponseEntity.ok(ApiResponse.success(url, "Thumbnail URL generated successfully"));
    }

    private ImageUploadResponse mapToResponse(Map<String, Object> result) {
        return ImageUploadResponse.builder()
                .url((String) result.get("url"))
                .publicId((String) result.get("publicId"))
                .format((String) result.get("format"))
                .width(result.get("width") != null ? ((Number) result.get("width")).intValue() : null)
                .height(result.get("height") != null ? ((Number) result.get("height")).intValue() : null)
                .bytes(result.get("bytes") != null ? ((Number) result.get("bytes")).longValue() : null)
                .build();
    }
}
