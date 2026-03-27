package com.example.mediaservice.service;

import com.example.mediaservice.model.Media;
import com.example.mediaservice.repository.MediaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class MediaService {
    
    @Value("${media.upload.dir:uploads/images}")
    private String uploadDir;
    
    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileValidator fileValidator;
    
    @Autowired
    private AuditService auditService;
    
    public Media uploadImage(MultipartFile file, String userId, String productId) throws IOException {
        // Validate file (size, type, content)
        fileValidator.validate(file);
        
        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate sanitized, unique filename
        String originalFilename = file.getOriginalFilename();
        String filename = fileValidator.sanitizeAndGenerateNewFilename(
            originalFilename != null ? originalFilename : "image"
        );
        Path filePath = uploadPath.resolve(filename);
        
        // Save file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Save metadata to database
        Media media = new Media();
        media.setImagePath(filePath.toString());
        media.setUserId(userId);
        media.setProductId(productId);
        media.setFilename(filename);
        media.setContentType(file.getContentType());
        media.setSize(file.getSize());
        
        Media savedMedia = mediaRepository.save(media);

        // Log audit event
        auditService.logWriteOperation(userId, "CREATE", "Media", savedMedia.getId(),
            "filename=" + filename + ", size=" + file.getSize() + " bytes");

        // Publish event to Kafka with mediaId, productId and userId
        try {
            String eventJson = objectMapper.writeValueAsString(java.util.Map.of(
                    "mediaId", savedMedia.getId(),
                    "productId", productId,
                    "userId", userId
            ));
            kafkaTemplate.send("image-uploaded", eventJson);
        } catch (JsonProcessingException e) {
            // If event serialization fails, continue without blocking upload
        }

        return savedMedia;
    }
    
    public byte[] getImage(String id) throws IOException {
        Media media = mediaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Media not found"));
        Path imagePath = Paths.get(media.getImagePath());
        if (!Files.exists(imagePath)) {
            throw new RuntimeException("Image file not found on disk");
        }
        return Files.readAllBytes(imagePath);
    }
    
    public Media getMediaById(String id) {
        return mediaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Media not found"));
    }
    
    public void deleteImage(String id, String userId) throws IOException {
        Media media = mediaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Media not found"));
        
        if (!media.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this media");
        }
        
        // Delete file from disk
        Path filePath = Paths.get(media.getImagePath());
        Files.deleteIfExists(filePath);
        
        // Delete from database
        mediaRepository.deleteById(id);
        
        // Log audit event
        auditService.logWriteOperation(userId, "DELETE", "Media", id,
            "filename=" + media.getFilename());
    }
    
    public List<Media> getMediaByProduct(String productId) {
        return mediaRepository.findByProductId(productId);
    }
    
    public List<Media> getMediaByUser(String userId) {
        return mediaRepository.findByUserId(userId);
    }

    public Page<Media> getMediaByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return mediaRepository.findByUserId(userId, pageable);
    }
}
