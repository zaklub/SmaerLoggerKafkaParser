package com.example.kafkaparsing.service;

import com.example.kafkaparsing.entity.ApiMetadata;
import com.example.kafkaparsing.entity.ApiMetadataField;
import com.example.kafkaparsing.repository.ApiMetadataFieldRepository;
import com.example.kafkaparsing.repository.ApiMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for API Metadata operations
 */
@Service
public class ApiMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(ApiMetadataService.class);

    @Autowired
    private ApiMetadataRepository apiMetadataRepository;

    @Autowired
    private ApiMetadataFieldRepository apiMetadataFieldRepository;

    /**
     * Find API metadata by API name
     */
    public Optional<ApiMetadata> findByApiName(String apiName) {
        try {
            logger.debug("Looking up API metadata for: {}", apiName);
            Optional<ApiMetadata> metadata = apiMetadataRepository.findByApiNameIgnoreCase(apiName);
            
            if (metadata.isPresent()) {
                logger.info("✅ Found API metadata for: {}", apiName);
            } else {
                logger.warn("⚠️ No API metadata found for: {}", apiName);
            }
            
            return metadata;
        } catch (Exception e) {
            logger.error("❌ Error finding API metadata for {}: {}", apiName, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Get all fields for an API metadata ID
     */
    public List<ApiMetadataField> getFieldsByApiMetadataId(java.util.UUID apiMetadataId) {
        try {
            logger.debug("Loading fields for API metadata ID: {}", apiMetadataId);
            List<ApiMetadataField> fields = apiMetadataFieldRepository.findByApiMetadataId(apiMetadataId);
            logger.info("✅ Found {} fields for API metadata ID: {}", fields.size(), apiMetadataId);
            return fields;
        } catch (Exception e) {
            logger.error("❌ Error loading fields for API metadata ID {}: {}", apiMetadataId, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Get mandatory fields for an API metadata ID
     */
    public List<ApiMetadataField> getMandatoryFieldsByApiMetadataId(java.util.UUID apiMetadataId) {
        try {
            logger.debug("Loading mandatory fields for API metadata ID: {}", apiMetadataId);
            List<ApiMetadataField> fields = apiMetadataFieldRepository.findByApiMetadataIdAndKeyStatus(apiMetadataId, "Mandatory");
            logger.info("✅ Found {} mandatory fields for API metadata ID: {}", fields.size(), apiMetadataId);
            return fields;
        } catch (Exception e) {
            logger.error("❌ Error loading mandatory fields for API metadata ID {}: {}", apiMetadataId, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Get field configuration for an API name (returns ALL fields - Mandatory + Custom)
     */
    public List<ApiMetadataField> getFieldConfigurationForApi(String apiName) {
        try {
            logger.info("📋 Loading field configuration for API: {}", apiName);
            
            // Find API metadata
            Optional<ApiMetadata> metadataOpt = findByApiName(apiName);
            
            if (!metadataOpt.isPresent()) {
                logger.warn("⚠️ No API metadata found for: {}", apiName);
                return java.util.Collections.emptyList();
            }
            
            ApiMetadata metadata = metadataOpt.get();
            
            // Get ALL fields (Mandatory + Custom)
            List<ApiMetadataField> fields = getFieldsByApiMetadataId(metadata.getUniqueId());
            
            // Count by type
            long mandatoryCount = fields.stream().filter(f -> "Mandatory".equals(f.getKeyStatus())).count();
            long customCount = fields.stream().filter(f -> "Custom".equals(f.getKeyStatus())).count();
            
            logger.info("📊 Field configuration loaded for API '{}': {} total fields ({} Mandatory, {} Custom)", 
                apiName, fields.size(), mandatoryCount, customCount);
            
            for (ApiMetadataField field : fields) {
                logger.debug("   - Field: {}, Path: {}, KeyStatus: {}, MessageType: {}, Datatype: {}", 
                    field.getField(), field.getPath(), field.getKeyStatus(), 
                    field.getMessageType() != null ? field.getMessageType() : "ANY", field.getDatatype());
            }
            
            return fields;
            
        } catch (Exception e) {
            logger.error("❌ Error getting field configuration for API {}: {}", apiName, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }
}

