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
	private FileValidator fileValidator;

	@Mock
	private MultipartFile mockFile;

	@InjectMocks
	private MediaService mediaService;

	@Test
	void getMediaById_throwsWhenMediaNotFound() {
		// Arrange
		String mediaId = "media-notfound";
		when(mediaRepository.findById(mediaId)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> mediaService.getMediaById(mediaId))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("not found");
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
