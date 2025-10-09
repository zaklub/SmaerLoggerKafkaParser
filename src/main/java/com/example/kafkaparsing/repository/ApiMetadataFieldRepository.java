package com.example.kafkaparsing.repository;

import com.example.kafkaparsing.entity.ApiMetadataField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ApiMetadataField entity
 */
@Repository
public interface ApiMetadataFieldRepository extends JpaRepository<ApiMetadataField, Long> {

    /**
     * Find all fields by API metadata ID
     */
    List<ApiMetadataField> findByApiMetadataId(UUID apiMetadataId);

    /**
     * Find mandatory fields by API metadata ID
     */
    List<ApiMetadataField> findByApiMetadataIdAndKeyStatus(UUID apiMetadataId, String keyStatus);
}

