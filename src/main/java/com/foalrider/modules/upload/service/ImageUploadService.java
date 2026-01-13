package com.foalrider.modules.upload.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Service interface for image upload operations.
 */
public interface ImageUploadService {

    /**
     * Upload a single image.
     *
     * @param file   The image file to upload
     * @param folder The folder to upload to (e.g., "products", "users")
     * @return Map containing upload result (url, publicId, etc.)
     */
    Map<String, Object> uploadImage(MultipartFile file, String folder);

    /**
     * Upload multiple images.
     *
     * @param files  List of image files to upload
     * @param folder The folder to upload to
     * @return List of upload results
     */
    List<Map<String, Object>> uploadImages(List<MultipartFile> files, String folder);

    /**
     * Delete an image by public ID.
     *
     * @param publicId The public ID of the image to delete
     * @return true if deletion was successful
     */
    boolean deleteImage(String publicId);

    /**
     * Delete multiple images.
     *
     * @param publicIds List of public IDs to delete
     * @return Number of successfully deleted images
     */
    int deleteImages(List<String> publicIds);

    /**
     * Generate a transformed URL for an image.
     *
     * @param publicId The public ID of the image
     * @param width    Desired width
     * @param height   Desired height
     * @return Transformed image URL
     */
    String getTransformedUrl(String publicId, int width, int height);

    /**
     * Generate a thumbnail URL.
     *
     * @param publicId The public ID of the image
     * @return Thumbnail URL (150x150)
     */
    String getThumbnailUrl(String publicId);
}
