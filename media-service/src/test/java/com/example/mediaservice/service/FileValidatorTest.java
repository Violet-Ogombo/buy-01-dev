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
    void validate_acceptsLargeFile() {
        byte[] largeContent = new byte[3 * 1024 * 1024]; // 3MB
        // Add valid JPEG header
        largeContent[0] = (byte) 0xFF;
        largeContent[1] = (byte) 0xD8;
        largeContent[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        // Should not throw any exception for large files
        assertDoesNotThrow(() -> fileValidator.validate(file));
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
    void validate_rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                new byte[0]
        );

        Exception exception = assertThrows(IllegalArgumentException.class, () -> fileValidator.validate(file));
        assertTrue(exception.getMessage().contains("File is empty"));
    }

    @Test
    void validate_acceptsJpegImage() {
        byte[] jpegHeader = new byte[] {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00
        };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.jpg",
                "image/jpeg",
                jpegHeader
        );

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_acceptsGifImage() {
        byte[] gifHeader = new byte[] {
            'G', 'I', 'F', '8', '9', 'a', 0x00, 0x00
        };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.gif",
                "image/gif",
                gifHeader
        );

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_acceptsGif87Image() {
        byte[] gif87Header = new byte[] {
            'G', 'I', 'F', '8', '7', 'a', 0x00, 0x00
        };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.gif",
                "image/gif",
                gif87Header
        );

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_acceptsWebpImage() {
        byte[] webpHeader = new byte[] {
            'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00,
            'W', 'E', 'B', 'P'
        };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.webp",
                "image/webp",
                webpHeader
        );

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_rejectsNullContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                null,
                new byte[100]
        );

        Exception exception = assertThrows(IllegalArgumentException.class, () -> fileValidator.validate(file));
        assertTrue(exception.getMessage().contains("Invalid file type"));
    }

    @Test
    void validate_acceptsLowercaseContentType() {
        byte[] jpegHeader = new byte[] {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00
        };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.jpg",
                "IMAGE/JPEG",  // Uppercase
                jpegHeader
        );

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_rejectsTruncatedPngHeader() {
        byte[] truncatedPng = new byte[] {
            (byte) 0x89, 0x50, 0x4E  // Only 3 bytes instead of 8
        };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                truncatedPng
        );

        Exception exception = assertThrows(IllegalArgumentException.class, () -> fileValidator.validate(file));
        assertTrue(exception.getMessage().contains("Invalid image content"));
    }

    @Test
    void validate_rejectsTruncatedWebpHeader() {
        byte[] truncatedWebp = new byte[] {
            'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00,
            'W', 'E', 'B'  // Missing 'P'
        };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.webp",
                "image/webp",
                truncatedWebp
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

    @Test
    void sanitizeAndGenerateNewFilename_removesParentDirectoryReferences() {
        String newName = fileValidator.sanitizeAndGenerateNewFilename("../../../etc/passwd");
        assertFalse(newName.contains(".."));
        assertFalse(newName.contains("/"));
    }

    @Test
    void sanitizeAndGenerateNewFilename_handlesWindowsPathSeparators() {
        String newName = fileValidator.sanitizeAndGenerateNewFilename("C:\\Users\\Admin\\image.jpg");
        assertTrue(newName.endsWith(".jpg"));
        assertFalse(newName.contains("\\"));
        assertFalse(newName.contains("C:"));
    }

    @Test
    void sanitizeAndGenerateNewFilename_preservesExtensionWithMultipleDots() {
        String newName = fileValidator.sanitizeAndGenerateNewFilename("image.backup.png");
        assertTrue(newName.endsWith(".png"));
    }

    @Test
    void sanitizeAndGenerateNewFilename_generatesUniqueFilenames() {
        String name1 = fileValidator.sanitizeAndGenerateNewFilename("image.jpg");
        String name2 = fileValidator.sanitizeAndGenerateNewFilename("image.jpg");
        
        assertNotEquals(name1, name2);
        assertTrue(name1.endsWith(".jpg"));
        assertTrue(name2.endsWith(".jpg"));
    }

    @Test
    void sanitizeAndGenerateNewFilename_handlesMissingExtension() {
        String newName = fileValidator.sanitizeAndGenerateNewFilename("imagefile");
        assertFalse(newName.contains("."));
        assertNotNull(newName);
        assertFalse(newName.isEmpty());
    }

    @Test
    void sanitizeAndGenerateNewFilename_throwsException_forNullFilename() {
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> fileValidator.sanitizeAndGenerateNewFilename(null));
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    void sanitizeAndGenerateNewFilename_throwsException_forBlankFilename() {
        Exception exception = assertThrows(IllegalArgumentException.class, 
            () -> fileValidator.sanitizeAndGenerateNewFilename("   "));
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    void sanitizeAndGenerateNewFilename_handlesDotAtEnd() {
        String newName = fileValidator.sanitizeAndGenerateNewFilename("image.");
        // Extension is empty when dot is at the end with no suffix
        assertFalse(newName.contains("."));
    }

    @Test
    void sanitizeAndGenerateNewFilename_handlesSingleCharacterExtension() {
        String newName = fileValidator.sanitizeAndGenerateNewFilename("image.x");
        assertTrue(newName.endsWith(".x"));
    }

    @Test
    void sanitizeAndGenerateNewFilename_keepsLastPathSegment() {
        String newName = fileValidator.sanitizeAndGenerateNewFilename("/path/to/image.png");
        assertTrue(newName.endsWith(".png"));
        assertFalse(newName.contains("/"));
        assertFalse(newName.contains("path"));
        assertFalse(newName.contains("to"));
    }
}

