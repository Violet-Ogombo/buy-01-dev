package com.example.mediaservice;

import com.example.mediaservice.repository.MediaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MediaRepository mediaRepository;

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
    @WithMockUser(roles = "SELLER")
    void getMyUploadsEndpointExists() throws Exception {
        mockMvc.perform(get("/images/my-uploads")
                .header("X-User-Id", "test-user-123"))
                .andExpect(status().isOk());
    }
}

