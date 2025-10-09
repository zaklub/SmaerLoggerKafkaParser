# Smart Logger Kafka Parser

A Spring Boot application that dynamically consumes messages from Kafka topics, performs Request/Response correlation, and indexes data to Elasticsearch with database-driven field mapping.

## Features

### 🔄 Dynamic Kafka Consumer Management
- Reads Kafka connection configurations from PostgreSQL `connections` table
- Dynamically creates and manages Kafka consumers based on database configurations
- Adds connection name and API name to all consumed messages
- Forwards enhanced messages to `raw-data-topic_kafka`

### 📊 Database-Driven Field Mapping
- Field mappings configured in `api_metadata` and `api_metadata_field` tables
- Supports both **Mandatory** and **Custom** field types
- Message-type aware extraction (REQUEST vs RESPONSE)
- Handles any JSON structure dynamically using JSON path expressions

### 🔗 Request/Response Correlation
- Correlates REQUEST and RESPONSE messages using correlation ID
- Configurable timeout (default: 1 minute)
- Handles orphaned messages gracefully
- Merges REQUEST + RESPONSE data before indexing

### 🔍 Elasticsearch Integration
- Indexes correlated data to `my_smartlogger_index`
- Mandatory fields mapped directly
- Custom fields stored as nested key-value pairs
- Supports custom date patterns for flexible date parsing

## Architecture

### Message Flow

```
Kafka Source Topic
         ↓
DynamicKafkaConsumerManager
         ↓
Enhance Message (+ connectionName + extractedApiName)
         ↓
raw-data-topic_kafka
         ↓
DynamicMessageProcessor
         ↓
Query api_metadata & api_metadata_field tables
         ↓
Extract fields using JSON paths
         ↓
Correlate REQUEST + RESPONSE (1 min timeout)
         ↓
Index to Elasticsearch (my_smartlogger_index)
```

## Database Schema

### 1. `connections` Table
Stores Kafka connection details:

```sql
CREATE TABLE connections (
    uniqueid uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    connectionname varchar(255) NOT NULL,
    connectiontype varchar(255) NOT NULL,
    details varchar(2000) NOT NULL,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);
```

**Details JSON Structure:**
```json
{
  "kafkaBrokers": ["localhost:9092"],
  "connectionName": "Kafka Frontend",
  "topic": "api_audit_zak_logs",
  "consumerGroupId": "local-dev-consumer",
  "securityProtocol": "PLAINTEXT",
  "certificate": null,
  "userName": "",
  "password": ""
}
```

### 2. `api_metadata` Table
Stores API configuration:

```sql
CREATE TABLE api_metadata (
    unique_id uuid PRIMARY KEY,
    api_name varchar(255) NOT NULL,
    connection_name varchar(255),
    dataset varchar(255),
    resource_path varchar(255),
    role_names varchar(255),
    status varchar(50),
    api_content_type varchar(50)
);
```

### 3. `api_metadata_field` Table
Stores field mapping configuration:

```sql
CREATE TABLE api_metadata_field (
    id bigserial PRIMARY KEY,
    content_type varchar(50),
    datatype varchar(50),
    field varchar(255) NOT NULL,
    identifier varchar(255),
    key_status varchar(50) NOT NULL,  -- 'Mandatory' or 'Custom'
    path varchar(500) NOT NULL,
    api_metadata_id uuid REFERENCES api_metadata(unique_id),
    date_pattern_string varchar(100),
    message_type varchar(50)  -- 'REQUEST', 'RESPONSE', or NULL (both)
);
```

## Elasticsearch Document Structure

### Mandatory Fields (Direct Mapping):
- `APIName`
- `CorrelationID`
- `Host`
- `ParentID`
- `RequestPayload`
- `RequestTime`
- `ResourcePath`
- `ResponsePayload`
- `ResponseTime`
- `Status`
- `StatusCode`
- `TransactionID`
- `UniqueTransactionID`

### Custom Fields (Nested Array):
```json
{
  "APIName": "GRN_CREATE_RECEIPT",
  "CorrelationID": "123-456-789",
  "Host": "127.0.0.1",
  "RequestTime": "2024-01-01T10:00:00",
  "ResponseTime": "2024-01-01T10:00:01",
  "Status": "SUCCESS",
  "StatusCode": 200,
  "CustomField": [
    {
      "key": "supplier_code",
      "value": "SUP001"
    },
    {
      "key": "total_amount",
      "value": "1000.0"
    }
  ]
}
```

## Docker Deployment

### Prerequisites
- Docker Engine 20.x or higher
- Docker Compose 2.x or higher
- PostgreSQL running on host (port 5432)
- Kafka running on host (port 9092)
- Elasticsearch running on host (port 9200)

### Quick Start

#### 1. Create configuration file:
```bash
# Copy the example config file
cp config.env.example config.env

# Edit config.env with your actual credentials
# nano config.env  (Linux/Mac)
# notepad config.env  (Windows)
```

#### 2. Build and start the application:
```bash
docker-compose up -d
```

This will start only the Spring Boot Application (port 8080) and connect to your existing services on the host machine.

#### 2. Check service health:
```bash
docker-compose ps
```

#### 3. View application logs:
```bash
docker-compose logs -f app
```

#### 4. Stop all services:
```bash
docker-compose down
```

#### 5. Stop and remove volumes (clean slate):
```bash
docker-compose down -v
```

### Environment Variables

The application supports these environment variables (configured in docker-compose.yml):

**Database:**
- `DB_HOST` - PostgreSQL host (default: host.docker.internal)
- `DB_PORT` - PostgreSQL port (default: 5432)
- `DB_NAME` - Database name (default: businessinsight)
- `DB_USERNAME` - Database user (default: audituser)
- `DB_PASSWORD` - Database password (default: manage)

**Kafka:**
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka broker address (default: host.docker.internal:9092)
- `KAFKA_CONSUMER_GROUP` - Consumer group ID (default: kafka-parser-consumer-group)

**Elasticsearch:**
- `ELASTICSEARCH_HOST` - Elasticsearch host (default: host.docker.internal)
- `ELASTICSEARCH_PORT` - Elasticsearch port (default: 9200)

**Note:** `host.docker.internal` allows the Docker container to connect to services running on the host machine.

## Local Development (Non-Docker)

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- PostgreSQL 12+
- Apache Kafka 2.8+
- Elasticsearch 7.x

### Running Locally
```bash
# Using Maven Wrapper (Windows)
.\mvnw.cmd spring-boot:run

# Using Maven Wrapper (Linux/Mac)
./mvnw spring-boot:run

# Using Maven
mvn spring-boot:run
```

## Configuration

### Database Setup

Before running, create the required database tables:

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create connections table
CREATE TABLE connections (
    uniqueid uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    connectionname varchar(255) NOT NULL,
    connectiontype varchar(255) NOT NULL,
    details varchar(2000) NOT NULL,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);

-- Create api_metadata table
CREATE TABLE api_metadata (
    unique_id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_name varchar(255) NOT NULL UNIQUE,
    connection_name varchar(255),
    dataset varchar(255),
    resource_path varchar(255),
    role_names varchar(255),
    status varchar(50),
    api_content_type varchar(50)
);

-- Create api_metadata_field table
CREATE TABLE api_metadata_field (
    id bigserial PRIMARY KEY,
    content_type varchar(50),
    datatype varchar(50),
    field varchar(255) NOT NULL,
    identifier varchar(255),
    key_status varchar(50) NOT NULL,
    path varchar(500) NOT NULL,
    api_metadata_id uuid REFERENCES api_metadata(unique_id),
    date_pattern_string varchar(100),
    message_type varchar(50)
);
```

### Insert Sample Configuration

```sql
-- Insert Kafka connection
INSERT INTO connections (connectionname, connectiontype, details)
VALUES (
    'Kafka Frontend',
    'kafka',
    '{"kafkaBrokers":["localhost:9092"],"topic":"api_audit_zak_logs","consumerGroupId":"local-dev-consumer","securityProtocol":"PLAINTEXT","userName":"","password":""}'
);

-- Insert API metadata
INSERT INTO api_metadata (unique_id, api_name, connection_name, resource_path, status, api_content_type)
VALUES (
    'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::uuid,
    'YOUR_API_NAME',
    'Kafka Frontend',
    '/api/your-endpoint',
    'ACTIVE',
    'JSON'
);

-- Insert field mappings
INSERT INTO api_metadata_field (field, path, key_status, message_type, api_metadata_id, datatype, content_type)
VALUES 
('APIName', 'api_name', 'Mandatory', NULL, 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::uuid, 'String', 'JSON'),
('CorrelationID', 'request_id', 'Mandatory', NULL, 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::uuid, 'String', 'JSON'),
('Host', 'metadata.client_ip', 'Mandatory', NULL, 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::uuid, 'String', 'JSON'),
('RequestTime', 'timestamp', 'Mandatory', 'REQUEST', 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::uuid, 'DateTime', 'JSON'),
('ResponseTime', 'payload.processed_date', 'Mandatory', 'RESPONSE', 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::uuid, 'DateTime', 'JSON'),
('Status', 'payload.status', 'Mandatory', 'RESPONSE', 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::uuid, 'String', 'JSON'),
('StatusCode', 'metadata.response_status', 'Mandatory', 'RESPONSE', 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::uuid, 'Integer', 'JSON');

-- Insert custom fields (only from REQUEST)
INSERT INTO api_metadata_field (field, path, key_status, message_type, api_metadata_id, datatype, content_type)
VALUES 
('supplier_code', 'payload.supplier_code', 'Custom', 'REQUEST', 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::uuid, 'String', 'JSON'),
('order_amount', 'payload.total_amount', 'Custom', 'REQUEST', 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::uuid, 'String', 'JSON');
```

### Create Elasticsearch Index

```bash
# Create my_smartlogger_index with proper mappings
curl -X PUT "http://localhost:9200/my_smartlogger_index" -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "properties": {
      "APIName": { "type": "keyword" },
      "CorrelationID": { "type": "keyword" },
      "Host": { "type": "keyword" },
      "ParentID": { "type": "keyword" },
      "RequestPayload": { "type": "text" },
      "RequestTime": { "type": "date", "format": "yyyy-MM-dd'\''T'\''HH:mm:ss" },
      "ResourcePath": { "type": "keyword" },
      "ResponsePayload": { "type": "text" },
      "ResponseTime": { "type": "date", "format": "yyyy-MM-dd'\''T'\''HH:mm:ss" },
      "Status": { "type": "keyword" },
      "StatusCode": { "type": "integer" },
      "TransactionID": { "type": "keyword" },
      "UniqueTransactionID": { "type": "keyword" },
      "CustomField": {
        "type": "nested",
        "properties": {
          "key": { "type": "keyword" },
          "value": { "type": "text" }
        }
      },
      "indexedAt": { "type": "date" },
      "isComplete": { "type": "boolean" }
    }
  }
}
'
```

## API Endpoints

### Audit Processor
- `GET /api/audit-processor/stats` - Get processor statistics (pending/completed messages)
- `GET /api/audit-processor/health` - Health check

## Monitoring

### Check Elasticsearch Data
```bash
# Get document count
curl -X GET "http://localhost:9200/my_smartlogger_index/_count?pretty"

# Search all documents
curl -X GET "http://localhost:9200/my_smartlogger_index/_search?pretty"

# Search by API name
curl -X GET "http://localhost:9200/my_smartlogger_index/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "term": {
      "APIName": "YOUR_API_NAME"
    }
  }
}
'
```

### Check Application Stats
```bash
curl -X GET "http://localhost:8080/api/audit-processor/stats"
```

## Configuration

### Timeout Settings (application.yml)
```yaml
audit:
  processor:
    timeout-minutes: 1  # How long to wait for Request/Response correlation
    cleanup-interval-minutes: 5  # How often to cleanup old transactions
```

## Key Components

### Services
- **DynamicKafkaConsumerManager** - Creates and manages Kafka consumers from database config
- **DynamicMessageProcessor** - Processes messages with Request/Response correlation
- **ApiMetadataService** - Loads field configuration from database
- **ElasticsearchService** - Indexes data to Elasticsearch
- **DataSourceConnectionService** - Loads Kafka connection details

### Consumers
- **ApiAuditConsumer** - Consumes from `api_audit_zak_logs`, forwards to `raw-data-topic_kafka`
- **RawDataConsumer** - Logs messages from `raw-data-topic_kafka`

## Troubleshooting

### Application won't start
```bash
# Check service health
docker-compose ps

# View logs
docker-compose logs -f app
docker-compose logs -f postgres
docker-compose logs -f kafka
docker-compose logs -f elasticsearch
```

### Database connection issues
```bash
# Connect to PostgreSQL
docker exec -it kafka-parser-postgres psql -U audituser -d businessinsight

# List tables
\dt

# Check connections
SELECT * FROM connections;
```

### Kafka connection issues
```bash
# List topics
docker exec -it kafka-parser-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Consume from topic
docker exec -it kafka-parser-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic raw-data-topic_kafka --from-beginning
```

### Elasticsearch issues
```bash
# Check cluster health
curl -X GET "http://localhost:9200/_cluster/health?pretty"

# List indices
curl -X GET "http://localhost:9200/_cat/indices?v"
```

## Development

### Build only
```bash
docker-compose build
```

### Rebuild application
```bash
docker-compose up -d --build app
```

### Restart application only
```bash
docker-compose restart app
```

## License

This project is licensed under the MIT License.
