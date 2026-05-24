package com.example.mediaservice;

import com.example.mediaservice.exception.ImageFileNotFoundException;
import com.example.mediaservice.exception.MediaAccessDeniedException;
import com.example.mediaservice.exception.MediaNotFoundException;
import com.example.mediaservice.model.Media;
import com.example.mediaservice.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MediaController mediaController;

    private Media testMedia;

    @BeforeEach
    void setUp() {
        testMedia = new Media();
        testMedia.setId("media-123");
        testMedia.setFilename("test.jpg");
        testMedia.setSize(1024);
        testMedia.setContentType("image/jpeg");
        testMedia.setUserId("user-123");
        testMedia.setProductId("prod-123");
    }

    @Test
    void uploadImage_successfullyUploadsFile() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test content".getBytes());
        when(mediaService.uploadImage(any(MultipartFile.class), eq("user-123"), eq("prod-123")))
                .thenReturn(testMedia);

        ResponseEntity<Map<String, Object>> response = mediaController.uploadImage(file, "prod-123", "Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKeys("id", "filename", "productId", "size");
        assertThat(response.getBody().get("id")).isEqualTo("media-123");
        verify(mediaService).uploadImage(file, "user-123", "prod-123");
    }

    @Test
    void uploadImage_returnsUnauthorizedWhenNoUserId() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test content".getBytes());

        ResponseEntity<Map<String, Object>> response = mediaController.uploadImage(file, "prod-123", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void uploadImage_handlesBadRequest() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
        when(mediaService.uploadImage(any(MultipartFile.class), eq("user-123"), eq("prod-123")))
                .thenThrow(new IllegalArgumentException("File type not allowed"));

        ResponseEntity<Map<String, Object>> response = mediaController.uploadImage(file, "prod-123", "Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void uploadImage_handlesServerError() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test content".getBytes());
        when(mediaService.uploadImage(any(MultipartFile.class), eq("user-123"), eq("prod-123")))
                .thenThrow(new RuntimeException("Upload failed"));

        ResponseEntity<Map<String, Object>> response = mediaController.uploadImage(file, "prod-123", "Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void getImage_returnsImageBytesSuccessfully() throws Exception {
        byte[] imageBytes = "test image content".getBytes();
        when(mediaService.getImage("media-123")).thenReturn(imageBytes);
        when(mediaService.getMediaById("media-123")).thenReturn(testMedia);

        ResponseEntity<byte[]> response = mediaController.getImage("media-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(imageBytes);
        assertThat(response.getHeaders().getContentType().toString()).contains("image/jpeg");
    }

    @Test
    void getImage_returnsNotFoundWhenImageDoesNotExist() throws Exception {
        when(mediaService.getImage("nonexistent")).thenThrow(new ImageFileNotFoundException("Not found"));

        ResponseEntity<byte[]> response = mediaController.getImage("nonexistent");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getImageMeta_returnsMediaMetadata() {
        when(mediaService.getMediaById("media-123")).thenReturn(testMedia);

        ResponseEntity<Media> response = mediaController.getImageMeta("media-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("media-123");
        assertThat(response.getBody().getFilename()).isEqualTo("test.jpg");
    }

    @Test
    void getImageMeta_returnsNotFoundWhenMediaNotFound() {
        when(mediaService.getMediaById("nonexistent")).thenThrow(new MediaNotFoundException("Not found"));

        ResponseEntity<Media> response = mediaController.getImageMeta("nonexistent");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteImage_deletesImageSuccessfully() throws IOException {
        ResponseEntity<Object> response = mediaController.deleteImage("media-123", "user-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(mediaService).deleteImage("media-123", "user-123");
    }

    @Test
    void deleteImage_returnsForbiddenWhenUnauthorized() throws IOException {
        doThrow(new MediaAccessDeniedException("Unauthorized"))
                .when(mediaService).deleteImage("media-123", "user-123");

        ResponseEntity<Object> response = mediaController.deleteImage("media-123", "user-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isInstanceOf(Map.class);
    }

    @Test
    void deleteImage_returnsNotFoundWhenMediaNotFound() throws IOException {
        doThrow(new MediaNotFoundException("Not found"))
                .when(mediaService).deleteImage("nonexistent", "user-123");

        ResponseEntity<Object> response = mediaController.deleteImage("nonexistent", "user-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getProductImages_returnsListOfMediaForProduct() {
        List<Media> mediaList = List.of(testMedia);
        when(mediaService.getMediaByProduct("prod-123")).thenReturn(mediaList);

        ResponseEntity<List<Media>> response = mediaController.getProductImages("prod-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo("media-123");
    }

    @Test
    void getProductImages_returnsEmptyListWhenNoMediaFound() {
        when(mediaService.getMediaByProduct("prod-123")).thenReturn(List.of());

        ResponseEntity<List<Media>> response = mediaController.getProductImages("prod-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getMyUploads_returnsUserMediaWithPagination() {
        Page<Media> mediaPage = new PageImpl<>(List.of(testMedia), null, 1);
        when(mediaService.getMediaByUser("user-123", 0, 10)).thenReturn(mediaPage);

        ResponseEntity<Map<String, Object>> response = mediaController.getMyUploads("user-123", 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("content", "page", "size", "totalElements", "totalPages");
        assertThat(response.getBody().get("totalElements")).isEqualTo(1L);
    }

    @Test
    void getMyUploads_returnsDefaultPagination() {
        Page<Media> mediaPage = new PageImpl<>(List.of(), null, 0);
        when(mediaService.getMediaByUser("user-123", 0, 10)).thenReturn(mediaPage);

        ResponseEntity<Map<String, Object>> response = mediaController.getMyUploads("user-123", 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("page")).isEqualTo(0);
        assertThat(response.getBody().get("size")).isEqualTo(0);
    }
}
