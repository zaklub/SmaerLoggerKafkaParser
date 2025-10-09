package com.example.kafkaparsing.repository;

import com.example.kafkaparsing.entity.ApiMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ApiMetadata entity
 */
@Repository
public interface ApiMetadataRepository extends JpaRepository<ApiMetadata, UUID> {

    /**
     * Find API metadata by API name
     */
    Optional<ApiMetadata> findByApiName(String apiName);

    /**
     * Find API metadata by API name (case insensitive)
     */
    Optional<ApiMetadata> findByApiNameIgnoreCase(String apiName);
}

