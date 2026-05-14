package com.example.mediaservice.service;

import com.example.mediaservice.model.Media;
import com.example.mediaservice.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        // Initialize any required state
    }

    @Test
    void uploadMedia_successfullyCreatesMediaRecord() {
        // Arrange
        String userId = "user-123";
        String filename = "image.jpg";
        String fileUrl = "/uploads/image.jpg";
        String fileType = "image/jpeg";

        Media expectedMedia = new Media();
        expectedMedia.setId("media-123");
        expectedMedia.setUserId(userId);
        expectedMedia.setFilename(filename);
        expectedMedia.setFileUrl(fileUrl);
        expectedMedia.setFileType(fileType);
        expectedMedia.setUploadedAt(LocalDateTime.now());

        when(mediaRepository.save(any(Media.class))).thenReturn(expectedMedia);

        // Act
        Media result = mediaService.uploadMedia(userId, filename, fileUrl, fileType);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("media-123");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getFilename()).isEqualTo(filename);
        assertThat(result.getFileUrl()).isEqualTo(fileUrl);
        assertThat(result.getFileType()).isEqualTo(fileType);

        ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(mediaCaptor.capture());
        assertThat(mediaCaptor.getValue().getUserId()).isEqualTo(userId);

        verify(auditService).logWriteOperation(userId, "CREATE", "Media", "media-123", 
            "filename=" + filename);
    }

    @Test
    void getMediaById_returnsMediaWhenExists() {
        // Arrange
        String mediaId = "media-123";
        Media media = new Media();
        media.setId(mediaId);
        media.setFilename("image.jpg");
        media.setUserId("user-123");

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));

        // Act
        Optional<Media> result = mediaService.getMediaById(mediaId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(mediaId);
        assertThat(result.get().getFilename()).isEqualTo("image.jpg");
    }

    @Test
    void getMediaById_returnsEmptyWhenNotExists() {
        // Arrange
        String mediaId = "nonexistent-id";
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.empty());

        // Act
        Optional<Media> result = mediaService.getMediaById(mediaId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getUserMedia_returnsMediaForUser() {
        // Arrange
        String userId = "user-123";
        Pageable pageable = PageRequest.of(0, 10);

        Media media1 = new Media();
        media1.setId("media-1");
        media1.setUserId(userId);
        
        Media media2 = new Media();
        media2.setId("media-2");
        media2.setUserId(userId);

        when(mediaRepository.findByUserId(userId, pageable))
            .thenReturn(new PageImpl<>(List.of(media1, media2)));

        // Act
        List<Media> result = mediaService.getUserMedia(userId, 0, 10);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting("id").contains("media-1", "media-2");
    }

    @Test
    void deleteMedia_successfullyDeletesMediaRecord() {
        // Arrange
        String mediaId = "media-123";
        String userId = "user-123";

        Media media = new Media();
        media.setId(mediaId);
        media.setUserId(userId);
        media.setFilename("image.jpg");

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));

        // Act
        mediaService.deleteMedia(mediaId, userId);

        // Assert
        verify(mediaRepository).deleteById(mediaId);
        verify(auditService).logWriteOperation(userId, "DELETE", "Media", mediaId, 
            "filename=image.jpg");
    }

    @Test
    void deleteMedia_deniesAccessIfNotOwner() {
        // Arrange
        String mediaId = "media-123";
        String ownerUserId = "owner-user";
        String otherUserId = "other-user";

        Media media = new Media();
        media.setId(mediaId);
        media.setUserId(ownerUserId);

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));

        // Act & Assert
        assertThatThrownBy(() -> mediaService.deleteMedia(mediaId, otherUserId))
            .isInstanceOf(Exception.class);

        verify(mediaRepository, never()).deleteById(mediaId);
    }

    @Test
    void deleteMedia_throwsExceptionIfMediaNotFound() {
        // Arrange
        String mediaId = "nonexistent-id";
        String userId = "user-123";

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> mediaService.deleteMedia(mediaId, userId))
            .isInstanceOf(Exception.class);

        verify(mediaRepository, never()).deleteById(mediaId);
    }

    @Test
    void getMediaByIdAndUserId_returnsMediaWhenOwnerMatches() {
        // Arrange
        String mediaId = "media-123";
        String userId = "user-123";

        Media media = new Media();
        media.setId(mediaId);
        media.setUserId(userId);
        media.setFilename("image.jpg");

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));

        // Act
        Optional<Media> result = mediaService.getMediaByIdAndUserId(mediaId, userId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(mediaId);
    }

    @Test
    void getMediaByIdAndUserId_returnsEmptyWhenOwnerDoesNotMatch() {
        // Arrange
        String mediaId = "media-123";
        String mediaOwner = "owner-user";
        String otherUser = "other-user";

        Media media = new Media();
        media.setId(mediaId);
        media.setUserId(mediaOwner);

        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));

        // Act
        Optional<Media> result = mediaService.getMediaByIdAndUserId(mediaId, otherUser);

        // Assert
        assertThat(result).isEmpty();
    }
}
