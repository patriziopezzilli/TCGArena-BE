package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.Image;
import com.example.tcgbackend.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Images", description = "API for managing image uploads and retrieval")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/upload")
    @Operation(summary = "Upload an image", description = "Uploads an image file and associates it with an entity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or upload failed")
    })
    public ResponseEntity<?> uploadImage(
            @Parameter(description = "Image file to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Type of entity (e.g., card, user)") @RequestParam("entityType") String entityType,
            @Parameter(description = "ID of the entity") @RequestParam(value = "entityId", required = false) Long entityId) {
        try {
            // TODO: Get from authentication
            Long userId = 1L;
            Image image = imageService.uploadImage(file, userId, entityType, entityId);
            return ResponseEntity.ok(image);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to upload image"));
        }
    }

    @GetMapping("/{filename}")
    @Operation(summary = "Get image file", description = "Retrieves an image file by filename")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Image not found")
    })
    public ResponseEntity<Resource> getImage(@Parameter(description = "Filename of the image") @PathVariable String filename) {
        try {
            Path filePath = imageService.getImagePath(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Determine content type
                String contentType = "application/octet-stream";
                try {
                    contentType = Files.probeContentType(filePath);
                } catch (IOException e) {
                    // Use default
                }

                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get images by entity", description = "Retrieves all images associated with a specific entity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Images retrieved successfully")
    })
    public List<Image> getImagesByEntity(
            @Parameter(description = "Type of entity") @PathVariable String entityType,
            @Parameter(description = "ID of the entity") @PathVariable Long entityId) {
        return imageService.getImagesByEntity(entityType, entityId);
    }

    @GetMapping("/user")
    @Operation(summary = "Get user's uploaded images", description = "Retrieves all images uploaded by the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Images retrieved successfully")
    })
    public List<Image> getUserImages() {
        // TODO: Get from authentication
        Long userId = 1L;
        return imageService.getImagesByUser(userId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete image", description = "Deletes an image uploaded by the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Image not found or not owned by user")
    })
    public ResponseEntity<Map<String, String>> deleteImage(@Parameter(description = "ID of the image") @PathVariable Long id) {
        // TODO: Get from authentication
        Long userId = 1L;
        boolean deleted = imageService.deleteImage(id, userId);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Image deleted successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}