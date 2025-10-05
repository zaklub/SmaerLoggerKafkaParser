# Smart Logger Kafka Parser

A Spring Boot application that provides dynamic Kafka consumer management, PostgreSQL integration, Elasticsearch connectivity, and comprehensive API audit logging.

## Features

### 🔄 Dynamic Kafka Consumer Management
- Reads Kafka connection configurations from PostgreSQL `connections` table
- Dynamically creates and manages Kafka consumers based on database configurations
- Supports multiple Kafka brokers, topics, and consumer groups
- Automatic consumer lifecycle management

### 📊 PostgreSQL Integration
- Connects to PostgreSQL database for configuration management
- Supports `group_config` and `connections` tables
- JPA/Hibernate integration with Spring Data

### 🔍 Elasticsearch Integration
- Elasticsearch cluster health monitoring
- Index management and information retrieval
- Index template operations
- Mapping inspection capabilities

### 📝 API Audit Logging
- Comprehensive request/response audit logging
- All API calls are logged to `api_audit_zak_logs` Kafka topic
- Support for both JSON and XML payloads
- Request metadata capture (IP, user agent, processing time, etc.)

### 🚀 REST APIs

#### GRN (Goods Receipt Note) API
- `POST /api/grn/create-receipt` - Create GRN receipt with audit logging
- Supports both JSON and XML input/output
- Comprehensive audit trail

#### Kafka Producer APIs
- `POST /api/kafka/send` - Send custom Kafka messages
- `POST /api/kafka/send-simple` - Send simple string messages
- `POST /api/kafka/send-custom` - Send messages with custom configuration

#### Raw Data API
- `POST /api/raw/send` - Send raw HTTP payloads directly to Kafka
- Bypasses deserialization for raw data forwarding

#### Elasticsearch APIs
- `GET /api/elasticsearch/indices` - List all indices
- `GET /api/elasticsearch/indices/{indexName}` - Get index details
- `GET /api/elasticsearch/health` - Cluster health status
- `GET /api/elasticsearch/ping` - Test connectivity
- `GET /api/elasticsearch/templates` - List index templates
- `GET /api/elasticsearch/indices/{indexName}/mappings` - Get index mappings

#### Configuration APIs
- `GET /api/group-config` - PostgreSQL group configuration management
- `GET /api/connection-test` - Test Kafka connection parsing

## Message Flow

```
API Request → api_audit_zak_logs → raw-data-topic_kafka
     ↓              ↓                    ↓
  Audit Log    ApiAuditConsumer    RawDataConsumer
     ↓              ↓                    ↓
  Kafka Topic   Console Output    Console Output
```

## Configuration

### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/businessinsight
    username: audituser
    password: manage
    driver-class-name: org.postgresql.Driver
```

### Kafka Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: local-dev-consumer
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

### Elasticsearch Configuration
```yaml
spring:
  elasticsearch:
    host: localhost
    port: 9200
    connection-timeout: 5s
    socket-timeout: 60s
```

## Database Schema

### connections Table
```sql
CREATE TABLE IF NOT EXISTS public.connections (
    uniqueid uuid NOT NULL DEFAULT uuid_generate_v4(),
    connectionname character varying(255) NOT NULL,
    connectiontype character varying(255) NOT NULL,
    details character varying(2000) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);
```

### group_config Table
```sql
CREATE TABLE IF NOT EXISTS public.group_config (
    group_config_id numeric NOT NULL,
    group_name character varying,
    group_value character varying,
    group_type character varying,
    description character varying,
    CONSTRAINT group_config_pkey PRIMARY KEY (group_config_id)
);
```

## Kafka Connection Details JSON Structure
```json
{
  "kafkaBrokers": ["localhost:9092"],
  "connectionName": "Kafka Frontend",
  "topic": "api_audit_zak_logs",
  "consumerGroupId": "local-dev-consumer",
  "securityProtocol": "PLAINTEXT",
  "certificate": null,
  "userName": "",
  "password": "",
  "fields": [...],
  "patterns": {...}
}
```

## Getting Started

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- PostgreSQL 12+
- Apache Kafka 2.8+
- Elasticsearch 7.x

### Running the Application
```bash
# Using Maven Wrapper
./mvnw spring-boot:run

# Or using Maven
mvn spring-boot:run
```

### Testing the APIs

#### GRN Create Receipt (JSON)
```bash
curl -X POST http://localhost:8080/api/grn/create-receipt \
  -H "Content-Type: application/json" \
  -d '{
    "grn_number": "GRN-2024-001",
    "supplier_code": "SUP001",
    "supplier_name": "ABC Corp",
    "po_number": "PO-001",
    "receipt_date": "2024-01-01T10:00:00",
    "items": [
      {
        "item_code": "ITEM001",
        "item_name": "Product A",
        "quantity": 10,
        "unit_price": 100.0
      }
    ],
    "total_amount": 1000.0,
    "currency": "USD",
    "remarks": "Test receipt"
  }'
```

#### GRN Create Receipt (XML)
```bash
curl -X POST http://localhost:8080/api/grn/create-receipt \
  -H "Content-Type: application/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<grnCreateReceiptRequest>
  <grnNumber>GRN-2024-001</grnNumber>
  <supplierCode>SUP001</supplierCode>
  <supplierName>ABC Corp</supplierName>
  <poNumber>PO-001</poNumber>
  <receiptDate>2024-01-01T10:00:00</receiptDate>
  <items>
    <item>
      <itemCode>ITEM001</itemCode>
      <itemName>Product A</itemName>
      <quantity>10</quantity>
      <unitPrice>100.0</unitPrice>
    </item>
  </items>
  <totalAmount>1000.0</totalAmount>
  <currency>USD</currency>
  <remarks>Test receipt</remarks>
</grnCreateReceiptRequest>'
```

#### Elasticsearch Health Check
```bash
curl -X GET http://localhost:8080/api/elasticsearch/health
```

#### List Elasticsearch Indices
```bash
curl -X GET http://localhost:8080/api/elasticsearch/indices
```

## Architecture

### Components
- **Controllers**: REST API endpoints
- **Services**: Business logic and external integrations
- **Repositories**: Data access layer
- **Entities**: JPA entities for database mapping
- **Models**: DTOs and data transfer objects
- **Configurations**: Spring configuration classes
- **Consumers**: Kafka message consumers

### Key Services
- `ApiAuditService`: Handles API audit logging
- `KafkaProducerService`: Manages Kafka message production
- `ElasticsearchService`: Elasticsearch operations
- `DataSourceConnectionService`: Database connection management
- `DynamicKafkaConsumerManager`: Dynamic consumer lifecycle management
- `KafkaMessageForwarder`: Message forwarding with retry logic

## Monitoring and Logging

The application provides comprehensive logging for:
- API request/response cycles
- Kafka message production and consumption
- Database operations
- Elasticsearch interactions
- Error handling and retry mechanisms

All audit logs are sent to `api_audit_zak_logs` Kafka topic and forwarded to `raw-data-topic_kafka` for further processing.

## License

This project is licensed under the MIT License.
