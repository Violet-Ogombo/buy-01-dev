package com.example.mediaservice.service;

import com.example.mediaservice.model.Media;
import com.example.mediaservice.repository.MediaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MediaService - NO Spring context loaded
 */
@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

	@Mock
	private MediaRepository mediaRepository;

	@Mock
	private AuditService auditService;

	@Mock
	private MultipartFile mockFile;

	@InjectMocks
	private MediaService mediaService;

	@Test
	void uploadImage_successfullyCreatesMediaRecord() throws IOException {
		// Arrange
		String userId = "user-123";
		String productId = "prod-456";
		String filename = "photo.jpg";
		String contentType = "image/jpeg";
		long fileSize = 102400L;

		when(mockFile.getOriginalFilename()).thenReturn(filename);
		when(mockFile.getContentType()).thenReturn(contentType);
		when(mockFile.getSize()).thenReturn(fileSize);
		when(mockFile.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));

		Media savedMedia = new Media();
		savedMedia.setId("media-1");
		savedMedia.setUserId(userId);
		savedMedia.setProductId(productId);
		savedMedia.setFilename(filename);
		savedMedia.setContentType(contentType);
		savedMedia.setSize(fileSize);
		savedMedia.setImagePath("/uploads/photo.jpg");

		when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

		// Act
		Media result = mediaService.uploadImage(mockFile, userId, productId);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("media-1");
		assertThat(result.getUserId()).isEqualTo(userId);
		assertThat(result.getProductId()).isEqualTo(productId);
		assertThat(result.getFilename()).isEqualTo(filename);
		assertThat(result.getContentType()).isEqualTo(contentType);

		ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
		verify(mediaRepository).save(mediaCaptor.capture());
		Media capturedMedia = mediaCaptor.getValue();
		assertThat(capturedMedia.getUserId()).isEqualTo(userId);
		assertThat(capturedMedia.getProductId()).isEqualTo(productId);
	}

	@Test
	void getMediaById_returnsMediaWhenExists() {
		// Arrange
		String mediaId = "media-1";
		Media media = new Media();
		media.setId(mediaId);
		media.setUserId("user-123");
		media.setFilename("photo.jpg");

		when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));

		// Act
		Media result = mediaService.getMediaById(mediaId);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(mediaId);
		assertThat(result.getFilename()).isEqualTo("photo.jpg");
	}

	@Test
	void deleteImage_successfullyRemovesMediaForOwner() throws IOException {
		// Arrange
		String mediaId = "media-1";
		String userId = "user-123";

		Media media = new Media();
		media.setId(mediaId);
		media.setUserId(userId);
		media.setFilename("photo.jpg");
		media.setImagePath("/tmp/uploads/photo.jpg");

		when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));

		// Act
		mediaService.deleteImage(mediaId, userId);

		// Assert
		verify(mediaRepository).deleteById(mediaId);
		verify(auditService).logWriteOperation(userId, "DELETE", "Media", mediaId, 
			"filename=photo.jpg");
	}

	@Test
	void deleteImage_throwsWhenNotOwner() {
		// Arrange
		String mediaId = "media-1";
		String userId = "user-123";
		String otherUserId = "user-999";

		Media media = new Media();
		media.setId(mediaId);
		media.setUserId(userId);
		media.setImagePath("/tmp/uploads/photo.jpg");

		when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));

		// Act & Assert
		assertThatThrownBy(() -> mediaService.deleteImage(mediaId, otherUserId))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Unauthorized");
	}
}
