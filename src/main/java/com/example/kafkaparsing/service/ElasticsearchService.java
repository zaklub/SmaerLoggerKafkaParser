package com.example.kafkaparsing.service;

import com.example.kafkaparsing.model.ParsedAuditData;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class ElasticsearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchService.class);

    @Autowired
    private RestHighLevelClient elasticsearchClient;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Get all indices from Elasticsearch using direct REST call
     */
    public List<String> getAllIndices() {
        try {
            Request request = new Request("GET", "/_cat/indices?format=json");
            Response response = elasticsearchClient.getLowLevelClient().performRequest(request);
            
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            List<String> indices = new ArrayList<>();
            if (jsonNode.isArray()) {
                for (JsonNode indexNode : jsonNode) {
                    String indexName = indexNode.get("index").asText();
                    indices.add(indexName);
                }
            }
            
            logger.info("Retrieved {} indices from Elasticsearch", indices.size());
            return indices;
            
        } catch (IOException e) {
            logger.error("Error retrieving indices from Elasticsearch", e);
            throw new RuntimeException("Failed to retrieve indices from Elasticsearch", e);
        }
    }

    /**
     * Get detailed information about all indices using direct REST call
     */
    public Map<String, Object> getIndicesDetails() {
        try {
            Request request = new Request("GET", "/_cat/indices?format=json&v=true");
            Response response = elasticsearchClient.getLowLevelClient().performRequest(request);
            
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            Map<String, Object> details = new HashMap<>();
            List<Map<String, Object>> indicesList = new ArrayList<>();
            
            if (jsonNode.isArray()) {
                for (JsonNode indexNode : jsonNode) {
                    Map<String, Object> indexInfo = new HashMap<>();
                    indexInfo.put("index", indexNode.get("index").asText());
                    indexInfo.put("health", indexNode.get("health").asText());
                    indexInfo.put("status", indexNode.get("status").asText());
                    indexInfo.put("docs_count", indexNode.get("docs.count").asText());
                    indexInfo.put("store_size", indexNode.get("store.size").asText());
                    indicesList.add(indexInfo);
                }
            }
            
            details.put("indices", indicesList);
            details.put("count", indicesList.size());
            
            logger.info("Retrieved detailed information for {} indices", indicesList.size());
            return details;
            
        } catch (IOException e) {
            logger.error("Error retrieving indices details from Elasticsearch", e);
            throw new RuntimeException("Failed to retrieve indices details from Elasticsearch", e);
        }
    }

    /**
     * Get cluster health information
     */
    public Map<String, Object> getClusterHealth() {
        try {
            ClusterHealthRequest request = new ClusterHealthRequest();
            ClusterHealthResponse response = elasticsearchClient.cluster().health(request, RequestOptions.DEFAULT);
            
            Map<String, Object> health = Map.of(
                "clusterName", response.getClusterName(),
                "status", response.getStatus().toString(),
                "numberOfNodes", response.getNumberOfNodes(),
                "numberOfDataNodes", response.getNumberOfDataNodes(),
                "activePrimaryShards", response.getActivePrimaryShards(),
                "activeShards", response.getActiveShards(),
                "relocatingShards", response.getRelocatingShards(),
                "initializingShards", response.getInitializingShards(),
                "unassignedShards", response.getUnassignedShards()
            );
            
            logger.info("Retrieved cluster health: {}", response.getStatus());
            return health;
            
        } catch (IOException e) {
            logger.error("Error retrieving cluster health from Elasticsearch", e);
            throw new RuntimeException("Failed to retrieve cluster health from Elasticsearch", e);
        }
    }

    /**
     * Check if Elasticsearch is available
     */
    public boolean isElasticsearchAvailable() {
        try {
            boolean response = elasticsearchClient.ping(RequestOptions.DEFAULT);
            logger.info("Elasticsearch ping successful: {}", response);
            return response;
        } catch (IOException e) {
            logger.error("Elasticsearch ping failed", e);
            return false;
        }
    }

    /**
     * Get index templates using direct REST call
     */
    public List<String> getIndexTemplates() {
        try {
            Request request = new Request("GET", "/_cat/templates?format=json");
            Response response = elasticsearchClient.getLowLevelClient().performRequest(request);
            
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            List<String> templates = new ArrayList<>();
            if (jsonNode.isArray()) {
                for (JsonNode templateNode : jsonNode) {
                    String templateName = templateNode.get("name").asText();
                    templates.add(templateName);
                }
            }
            
            logger.info("Retrieved {} index templates", templates.size());
            return templates;
            
        } catch (IOException e) {
            logger.error("Error retrieving index templates from Elasticsearch", e);
            throw new RuntimeException("Failed to retrieve index templates from Elasticsearch", e);
        }
    }

    /**
     * Get mappings for a specific index
     */
    public Map<String, Object> getIndexMappings(String indexName) {
        try {
            Request request = new Request("GET", "/" + indexName + "/_mapping");
            Response response = elasticsearchClient.getLowLevelClient().performRequest(request);
            
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            Map<String, Object> result = new HashMap<>();
            result.put("index", indexName);
            result.put("mappings", objectMapper.convertValue(jsonNode, Map.class));
            
            logger.info("Retrieved mappings for index: {}", indexName);
            return result;
            
        } catch (IOException e) {
            logger.error("Error retrieving mappings for index {} from Elasticsearch", indexName, e);
            throw new RuntimeException("Failed to retrieve mappings for index " + indexName + " from Elasticsearch", e);
        }
    }

    /**
     * Index ParsedAuditData to my_smartlogger_index using low-level REST client
     */
    public void indexAuditData(ParsedAuditData parsedData) throws IOException {
        try {
            // Generate unique ID if not set
            if (parsedData.getId() == null) {
                parsedData.setId(UUID.randomUUID().toString());
            }

            // Convert to JSON
            String jsonData = objectMapper.writeValueAsString(parsedData);

            // Use low-level REST client to avoid parsing issues
            Request request = new Request("PUT", "/my_smartlogger_index/_doc/" + parsedData.getId());
            request.setJsonEntity(jsonData);
            
            // Execute index request
            Response response = elasticsearchClient.getLowLevelClient().performRequest(request);
            
            // Check if successful (201 Created or 200 OK)
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 201) {
                logger.info("📊 Successfully indexed audit data to my_smartlogger_index:");
                logger.info("   ID: {}", parsedData.getId());
                logger.info("   Status: {}", response.getStatusLine().getStatusCode());
                logger.info("   Correlation ID: {}", parsedData.getCorrelationId());
                logger.info("   Complete: {}", parsedData.getIsComplete());
                logger.info("   API Name: {}", parsedData.getApiName());
                logger.info("   Resource Path: {}", parsedData.getResourcePath());
            } else {
                throw new IOException("Unexpected status code: " + statusCode);
            }

        } catch (Exception e) {
            logger.error("❌ Failed to index audit data to my_smartlogger_index: {}", e.getMessage(), e);
            throw new IOException("Failed to index audit data", e);
        }
    }

    /**
     * Check if my_smartlogger_index exists
     */
    public boolean checkSmartLoggerIndexExists() {
        try {
            Request request = new Request("HEAD", "/my_smartlogger_index");
            Response response = elasticsearchClient.getLowLevelClient().performRequest(request);
            
            boolean exists = response.getStatusLine().getStatusCode() == 200;
            logger.info("🔍 my_smartlogger_index exists: {}", exists);
            return exists;
            
        } catch (IOException e) {
            logger.warn("⚠️ Error checking if my_smartlogger_index exists: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get information about my_smartlogger_index
     */
    public Map<String, Object> getSmartLoggerIndexInfo() {
        try {
            Request request = new Request("GET", "/my_smartlogger_index");
            Response response = elasticsearchClient.getLowLevelClient().performRequest(request);
            
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            Map<String, Object> indexInfo = new HashMap<>();
            if (jsonNode.has("my_smartlogger_index")) {
                JsonNode indexNode = jsonNode.get("my_smartlogger_index");
                JsonNode mappingsNode = indexNode.get("mappings");
                JsonNode settingsNode = indexNode.get("settings");
                
                indexInfo.put("mappings", mappingsNode);
                indexInfo.put("settings", settingsNode);
            }
            
            return indexInfo;
            
        } catch (IOException e) {
            logger.error("❌ Error getting my_smartlogger_index info: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Get document count for my_smartlogger_index
     */
    public long getSmartLoggerIndexDocumentCount() {
        try {
            Request request = new Request("GET", "/my_smartlogger_index/_count");
            Response response = elasticsearchClient.getLowLevelClient().performRequest(request);
            
            String responseBody = new String(response.getEntity().getContent().readAllBytes());
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            return jsonNode.get("count").asLong();
            
        } catch (IOException e) {
            logger.error("❌ Error getting my_smartlogger_index document count: {}", e.getMessage(), e);
            return -1;
        }
    }
}
