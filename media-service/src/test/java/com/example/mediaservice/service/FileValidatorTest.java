package com.example.mediaservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileValidatorTest {

    private final FileValidator fileValidator = new FileValidator();

    @Test
    void validate_acceptsValidImage() {
        byte[] pngHeader = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x00
        };
        MockMultipartFile file = new MockMultipartFile(
                "file",
            "image.png",
            "image/png",
            pngHeader
        );

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_rejectsTooLargeFile() {
        byte[] largeContent = new byte[3 * 1024 * 1024]; // 3MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        Exception exception = assertThrows(IllegalArgumentException.class, () -> fileValidator.validate(file));
        assertTrue(exception.getMessage().contains("File size exceeds"));
    }

    @Test
    void validate_rejectsInvalidContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.txt",
                "text/plain",
                new byte[1024]
        );

        Exception exception = assertThrows(IllegalArgumentException.class, () -> fileValidator.validate(file));
        assertTrue(exception.getMessage().contains("Invalid file type"));
    }

    @Test
    void validate_rejectsInvalidMagicBytes() {
        byte[] invalidContent = new byte[16];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                invalidContent
        );

        Exception exception = assertThrows(IllegalArgumentException.class, () -> fileValidator.validate(file));
        assertTrue(exception.getMessage().contains("Invalid image content"));
    }

    @Test
    void sanitizeAndGenerateNewFilename_generatesUuidWithExtension() {
        String newName = fileValidator.sanitizeAndGenerateNewFilename("../../evil/image.png");
        assertTrue(newName.endsWith(".png"));
        assertFalse(newName.contains(".."));
        assertFalse(newName.contains("/"));
        assertFalse(newName.contains("\\"));
    }
}
