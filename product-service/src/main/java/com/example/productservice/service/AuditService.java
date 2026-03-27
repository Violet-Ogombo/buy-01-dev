package com.example.productservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Log audit event for write operations (CREATE, UPDATE, DELETE)
     */
    public void logWriteOperation(String userId, String action, String entityType, String entityId, String details) {
        LocalDateTime timestamp = LocalDateTime.now();
        String timestampStr = timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // Log to application logger
        logger.info("AUDIT: userId={}, action={}, entity={}, entityId={}, timestamp={}, details={}",
                userId, action, entityType, entityId, timestampStr, details);
        
        // Publish to Kafka topic for centralized audit log
        try {
            Map<String, Object> auditEvent = Map.of(
                    "userId", userId,
                    "action", action,
                    "entityType", entityType,
                    "entityId", entityId,
                    "timestamp", timestampStr,
                    "details", details != null ? details : ""
            );
            String auditJson = objectMapper.writeValueAsString(auditEvent);
            kafkaTemplate.send("audit-log", auditJson);
        } catch (Exception e) {
            logger.error("Failed to publish audit event to Kafka", e);
        }
    }
}
