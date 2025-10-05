package com.example.kafkaparsing.service;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class ElasticsearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchService.class);

    @Autowired
    private RestHighLevelClient elasticsearchClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
}
