package com.foalrider.modules.upload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for image upload operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {

    private String url;
    private String publicId;
    private String format;
    private Integer width;
    private Integer height;
    private Long bytes;
}
