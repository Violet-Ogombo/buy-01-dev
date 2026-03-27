package com.example.mediaservice.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Component
public class FileValidator {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    public void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 50MB limit");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Only images (JPEG, PNG, GIF, WEBP) are allowed");
        }

        // Validate magic bytes (file signature) to prevent MIME type spoofing
        try (var is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read <= 0 || !isAllowedImageHeader(header, read)) {
                throw new IllegalArgumentException("Invalid image content");
            }
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Failed to read file content", e);
        }
    }

    private boolean isAllowedImageHeader(byte[] header, int length) {
        // JPEG: FF D8 FF
        if (length >= 3 &&
                (header[0] & 0xFF) == 0xFF &&
                (header[1] & 0xFF) == 0xD8 &&
                (header[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (length >= 8 &&
                (header[0] & 0xFF) == 0x89 &&
                header[1] == 0x50 &&
                header[2] == 0x4E &&
                header[3] == 0x47 &&
                header[4] == 0x0D &&
                header[5] == 0x0A &&
                header[6] == 0x1A &&
                header[7] == 0x0A) {
            return true;
        }
        // GIF: "GIF87a" or "GIF89a"
        if (length >= 6 &&
                header[0] == 'G' &&
                header[1] == 'I' &&
                header[2] == 'F' &&
                header[3] == '8' &&
                (header[4] == '7' || header[4] == '9') &&
                header[5] == 'a') {
            return true;
        }
        // WEBP: "RIFF....WEBP"
        if (length >= 12 &&
                header[0] == 'R' &&
                header[1] == 'I' &&
                header[2] == 'F' &&
                header[3] == 'F' &&
                header[8] == 'W' &&
                header[9] == 'E' &&
                header[10] == 'B' &&
                header[11] == 'P') {
            return true;
        }
        return false;
    }

    public String sanitizeAndGenerateNewFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Original filename is empty");
        }
        // Normalize separators
        String sanitized = originalFilename.replace("\\", "/");
        // Keep only the last path segment
        int lastSlash = sanitized.lastIndexOf('/');
        if (lastSlash >= 0) {
            sanitized = sanitized.substring(lastSlash + 1);
        }
        // Remove any remaining parent directory references
        sanitized = sanitized.replace("..", "");

        String extension = "";
        int dotIndex = sanitized.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < sanitized.length() - 1) {
            extension = sanitized.substring(dotIndex + 1);
        }

        String filename = UUID.randomUUID().toString();
        if (!extension.isEmpty()) {
            filename += "." + extension;
        }
        return filename;
    }
}
