package com.example.mediaservice.repository;

import com.example.mediaservice.model.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MediaRepository extends MongoRepository<Media, String> {
    List<Media> findByProductId(String productId);
    List<Media> findByUserId(String userId);
    Page<Media> findByUserId(String userId, Pageable pageable);
}
