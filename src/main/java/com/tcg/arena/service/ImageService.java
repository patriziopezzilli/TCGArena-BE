package com.tcg.arena.service;

import com.tcg.arena.model.Image;
import com.tcg.arena.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImageService {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageCompressionService imageCompressionService;

    private final String uploadDir = "uploads/images/";

    public Image uploadImage(MultipartFile file, Long uploadedBy, String entityType, Long entityId) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID().toString() + extension;

        // Compress the image
        String format = imageCompressionService.getImageFormat(originalFilename);
        byte[] compressedData = imageCompressionService.compressImage(file.getBytes(), format);

        // Save compressed file
        Path filePath = uploadPath.resolve(filename);
        Files.write(filePath, compressedData);

        // Create image record
        Image image = new Image();
        image.setFilename(filename);
        image.setOriginalFilename(originalFilename);
        image.setContentType(file.getContentType());
        image.setSize((long) compressedData.length);
        image.setUrl("/api/images/" + filename); // Assuming a controller serves images
        image.setUploadedBy(uploadedBy);
        image.setUploadedAt(LocalDateTime.now());
        image.setEntityType(entityType);
        image.setEntityId(entityId);

        return imageRepository.save(image);
    }

    public Optional<Image> getImageById(Long id) {
        return imageRepository.findById(id);
    }

    public List<Image> getImagesByEntity(String entityType, Long entityId) {
        return imageRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public List<Image> getImagesByUser(Long uploadedBy) {
        return imageRepository.findByUploadedBy(uploadedBy);
    }

    public boolean deleteImage(Long id, Long userId) {
        Optional<Image> imageOpt = imageRepository.findById(id);
        if (imageOpt.isPresent()) {
            Image image = imageOpt.get();
            if (image.getUploadedBy().equals(userId)) {
                // Delete file
                try {
                    Path filePath = Paths.get(uploadDir).resolve(image.getFilename());
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    // Log error but continue
                }
                imageRepository.delete(image);
                return true;
            }
        }
        return false;
    }
}