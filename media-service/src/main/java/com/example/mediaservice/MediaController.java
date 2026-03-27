package com.example.mediaservice;

import com.example.mediaservice.model.Media;
import com.example.mediaservice.service.MediaService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/images")
public class MediaController {
    private static final Logger log = LoggerFactory.getLogger(MediaController.class);
    
    @Autowired
    private MediaService mediaService;

    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @PostMapping
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("productId") String productId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        
        log.info("📸 uploadImage called - file: {}, productId: {}", 
                 file.getOriginalFilename(), productId);
        
        // Extract userId from JWT token in Authorization header OR from SecurityContext (API Gateway)
        String userId = extractUserIdFromJwt(authHeader);
        
        if (userId == null) {
            // Fallback: Try to get userId from SecurityContext (set by ApiGatewayAuthenticationFilter)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                userId = auth.getName();  // The username/userId from X-User-Id header
                log.info("✅ Using userId from SecurityContext: {}", userId);
            }
        }
        
        if (userId == null || userId.isEmpty()) {
            log.warn("❌ Unauthorized - cannot extract user ID from JWT token or SecurityContext");
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized - invalid or missing token"));
        }
        
        log.info("✅ Authentication successful - userId: {}", userId);
        
        try {
            log.info("✅ Uploading file: {} for product: {} by user: {}", 
                     file.getOriginalFilename(), productId, userId);
            Media media = mediaService.uploadImage(file, userId, productId);
            log.info("✅ File uploaded successfully - mediaId: {}", media.getId());
            return ResponseEntity.status(201).body(Map.of(
                "id", media.getId(),
                "filename", media.getFilename(),
                "productId", media.getProductId(),
                "size", media.getSize()
            ));
        } catch (IllegalArgumentException e) {
            log.error("❌ Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
    
    private String extractUserIdFromJwt(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("⚠️ No Bearer token in Authorization header");
            return null;
        }
        
        String token = authHeader.substring(7);
        try {
            @SuppressWarnings("null")
            byte[] keyBytes = jwtSecret != null ? jwtSecret.getBytes(StandardCharsets.UTF_8) : new byte[0];
            
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(keyBytes)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String userId = claims.get("userId", String.class);
            log.debug("✅ JWT parsed successfully - userId: {}", userId);
            return userId;
        } catch (Exception e) {
            log.warn("❌ JWT parse failed: {}", e.getMessage());
            return null;
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable String id) {
        try {
            byte[] imageBytes = mediaService.getImage(id);
            Media media = mediaService.getMediaById(id);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(media.getContentType()));
            headers.setCacheControl("public, max-age=31536000");
            headers.setETag("\"" + Integer.toHexString(java.util.Arrays.hashCode(imageBytes)) + "\"");
            
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/meta")
    public ResponseEntity<Media> getImageMeta(@PathVariable String id) {
        try {
            Media media = mediaService.getMediaById(id);
            return ResponseEntity.ok(media);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> deleteImage(@PathVariable String id,
                                        @RequestHeader("X-User-Id") String userId) {
        try {
            mediaService.deleteImage(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Unauthorized")) {
                return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Delete failed"));
        }
    }
    
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Media>> getProductImages(@PathVariable String productId) {
        List<Media> media = mediaService.getMediaByProduct(productId);
        return ResponseEntity.ok(media);
    }
    
    @GetMapping("/my-uploads")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Map<String, Object>> getMyUploads(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        var mediaPage = mediaService.getMediaByUser(userId, page, size);

        Map<String, Object> response = Map.of(
                "content", mediaPage.getContent(),
                "page", mediaPage.getNumber(),
                "size", mediaPage.getSize(),
                "totalElements", mediaPage.getTotalElements(),
                "totalPages", mediaPage.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }
}
