package com.example.mediaservice.service;

import com.example.mediaservice.exception.ImageFileNotFoundException;
import com.example.mediaservice.exception.MediaAccessDeniedException;
import com.example.mediaservice.exception.MediaNotFoundException;
import com.example.mediaservice.model.Media;
import com.example.mediaservice.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MediaService using manual mocking to avoid Mockito ByteBuddy
 * compatibility issues with Java 25.
 */
class MediaServiceTest {

    private MediaService mediaService;
    private FakeMediaRepository fakeMediaRepository;

    @BeforeEach
    void setUp() {
        mediaService = new MediaService();
        fakeMediaRepository = new FakeMediaRepository();
        ReflectionTestUtils.setField(mediaService, "mediaRepository", fakeMediaRepository);
        ReflectionTestUtils.setField(mediaService, "kafkaTemplate", null);
        ReflectionTestUtils.setField(mediaService, "auditService", new NullAuditService());
    }

    // === GET MEDIA BY ID TESTS ===

    @Test
    void getMediaById_returnsMedia_whenExists() {
        Media testMedia = new Media();
        testMedia.setId("media-123");
        testMedia.setFilename("test.jpg");
        fakeMediaRepository.save(testMedia);

        Media result = mediaService.getMediaById("media-123");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("media-123");
    }

    @Test
    void getMediaById_throwsException_whenNotFound() {
        assertThatThrownBy(() ->
            mediaService.getMediaById("nonexistent")
        ).isInstanceOf(MediaNotFoundException.class);
    }

    // === GET IMAGE TESTS ===

    @Test
    void getImage_throwsException_whenMediaNotFound() {
        assertThatThrownBy(() ->
            mediaService.getImage("nonexistent")
        ).isInstanceOf(MediaNotFoundException.class)
         .hasMessage("Media not found");
    }

    @Test
    void getImage_throwsException_whenImageFileNotFound() {
        Media testMedia = new Media();
        testMedia.setId("media-123");
        testMedia.setImagePath("/nonexistent/path/image.jpg");
        fakeMediaRepository.save(testMedia);

        assertThatThrownBy(() ->
            mediaService.getImage("media-123")
        ).isInstanceOf(ImageFileNotFoundException.class)
         .hasMessage("Image file not found on disk");
    }

    // === DELETE IMAGE TESTS ===

    @Test
    void deleteImage_throwsException_whenNotFound() {
        assertThatThrownBy(() ->
            mediaService.deleteImage("nonexistent", "user-789")
        ).isInstanceOf(MediaNotFoundException.class);
    }

    @Test
    void deleteImage_throwsException_whenUserNotOwner() {
        Media testMedia = new Media();
        testMedia.setId("media-123");
        testMedia.setUserId("user-789");
        fakeMediaRepository.save(testMedia);

        assertThatThrownBy(() ->
            mediaService.deleteImage("media-123", "different-user")
        ).isInstanceOf(MediaAccessDeniedException.class)
         .hasMessage("Unauthorized to delete this media");
    }

    // === GET MEDIA BY PRODUCT TESTS ===

    @Test
    void getMediaByProduct_returnsMediaList() {
        Media testMedia = new Media();
        testMedia.setId("media-123");
        testMedia.setProductId("product-456");
        fakeMediaRepository.save(testMedia);

        List<Media> result = mediaService.getMediaByProduct("product-456");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("media-123");
    }

    @Test
    void getMediaByProduct_returnsEmptyList() {
        List<Media> result = mediaService.getMediaByProduct("product-empty");

        assertThat(result).isEmpty();
    }

    // === GET MEDIA BY USER TESTS ===

    @Test
    void getMediaByUser_returnsList() {
        Media testMedia = new Media();
        testMedia.setId("media-123");
        testMedia.setUserId("user-789");
        fakeMediaRepository.save(testMedia);

        List<Media> result = mediaService.getMediaByUser("user-789");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user-789");
    }

    @Test
    void getMediaByUser_returnsPage() {
        Media testMedia = new Media();
        testMedia.setId("media-123");
        testMedia.setUserId("user-789");
        fakeMediaRepository.save(testMedia);

        Page<Media> result = mediaService.getMediaByUser("user-789", 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getNumber()).isEqualTo(0);
    }

    @Test
    void getMediaByUser_supportsPagination() {
        Page<Media> result = mediaService.getMediaByUser("user-789", 2, 20);

        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(20);
    }

    // === FAKE IMPLEMENTATIONS ===

    private static class FakeMediaRepository implements MediaRepository {
        private final Map<String, Media> media = new HashMap<>();

        @Override
        public Optional<Media> findById(String id) {
            return Optional.ofNullable(media.get(id));
        }

        @Override
        public <S extends Media> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId("media-" + System.nanoTime());
            }
            media.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public List<Media> findByProductId(String productId) {
            return media.values().stream()
                .filter(m -> productId.equals(m.getProductId()))
                .toList();
        }

        @Override
        public List<Media> findByUserId(String userId) {
            return media.values().stream()
                .filter(m -> userId.equals(m.getUserId()))
                .toList();
        }

        @Override
        public Page<Media> findByUserId(String userId, org.springframework.data.domain.Pageable pageable) {
            var list = findByUserId(userId);
            return new PageImpl<>(list, pageable, list.size());
        }

        @Override
        public void deleteById(String id) {
            media.remove(id);
        }

        // Stub implementations
        @Override
        public <S extends Media> S insert(S entity) { return save(entity); }
        @Override
        public <S extends Media> List<S> insert(Iterable<S> entities) { return List.of(); }
        @Override
        public <S extends Media> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override
        public boolean existsById(String s) { return false; }
        @Override
        public List<Media> findAll() { return List.copyOf(media.values()); }
        @Override
        public List<Media> findAllById(Iterable<String> strings) { return List.of(); }
        @Override
        public long count() { return media.size(); }
        @Override
        public void delete(Media entity) {}
        @Override
        public void deleteAllById(Iterable<? extends String> strings) {}
        @Override
        public void deleteAll(Iterable<? extends Media> entities) {}
        @Override
        public void deleteAll() {}
        @Override
        public <S extends Media> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        @Override
        public <S extends Media> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override
        public <S extends Media> Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return Page.empty(); }
        @Override
        public <S extends Media> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override
        public <S extends Media> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override
        public <S extends Media, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override
        public <S extends Media> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override
        public List<Media> findAll(org.springframework.data.domain.Sort sort) { return List.copyOf(media.values()); }
        @Override
        public Page<Media> findAll(org.springframework.data.domain.Pageable pageable) { return Page.empty(); }
    }

    private static class NullAuditService extends AuditService {
        @Override
        public void logWriteOperation(String userId, String operation, String resourceType, String resourceId, String details) {
            // No-op for testing
        }
    }
}
