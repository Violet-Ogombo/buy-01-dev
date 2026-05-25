package com.example.mediaservice;

import com.example.mediaservice.model.Media;
import com.example.mediaservice.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MediaIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MediaController controller = new MediaController();
        ReflectionTestUtils.setField(controller, "mediaService", new FakeMediaService());
        ReflectionTestUtils.setField(controller, "jwtSecret", "test-secret");
        
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void mediaEndpointExists() throws Exception {
        mockMvc.perform(get("/images/product/test-product"))
                .andExpect(status().isOk());
    }

    @Test
    void getImageReturnsNotFoundForNonExistent() throws Exception {
        mockMvc.perform(get("/images/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyUploadsEndpointExists() throws Exception {
        mockMvc.perform(get("/images/my-uploads").header("X-User-Id", "test-user-123"))
                .andExpect(status().isOk());
    }

    private static final class FakeMediaService extends MediaService {
        @Override
        public byte[] getImage(String mediaId) {
            return new byte[0];
        }

        @Override
        public Media getMediaById(String mediaId) {
            throw new RuntimeException("Media not found with id: " + mediaId);
        }

        @Override
        public List<Media> getMediaByProduct(String productId) {
            return new ArrayList<>();
        }

        @Override
        public Page<Media> getMediaByUser(String userId, int page, int size) {
            return new PageImpl<>(new ArrayList<>());
        }
    }
}

