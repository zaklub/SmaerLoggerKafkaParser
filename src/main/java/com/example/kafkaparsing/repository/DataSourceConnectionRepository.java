package com.example.kafkaparsing.repository;

import com.example.kafkaparsing.entity.DataSourceConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DataSourceConnectionRepository extends JpaRepository<DataSourceConnection, UUID> {

    /**
     * Find all connections by connection type (e.g., "kafka")
     */
    List<DataSourceConnection> findByConnectionType(String connectionType);

    /**
     * Find all Kafka connections (case-insensitive)
     */
    List<DataSourceConnection> findByConnectionTypeIgnoreCase(String connectionType);
}

