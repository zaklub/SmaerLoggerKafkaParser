# Smart Logger Kafka Parser

A Spring Boot application that dynamically consumes messages from Kafka topics, performs intelligent Request/Response correlation, and indexes structured data to Elasticsearch with database-driven field mapping.

---

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Complete Application Flow](#complete-application-flow)
4. [Database Schema](#database-schema)
5. [Elasticsearch Index Structure](#elasticsearch-index-structure)
6. [End-to-End Example](#end-to-end-example)
7. [Use Cases](#use-cases)
8. [Hardware Requirements](#hardware-requirements)
9. [Docker Deployment](#docker-deployment)
10. [Configuration Management](#configuration-management)
11. [API Endpoints](#api-endpoints)
12. [Troubleshooting](#troubleshooting)
13. [Technology Stack](#technology-stack)

---

## Overview

The Smart Logger Kafka Parser is a **production-ready, enterprise-grade message processing system** that:

✅ Dynamically consumes from multiple Kafka topics (configured in database)
✅ Intelligently correlates REQUEST and RESPONSE messages
✅ Extracts fields using database-driven JSON path mapping
✅ Supports both standard and custom business fields
✅ Indexes complete transaction data to Elasticsearch
✅ Provides comprehensive monitoring and troubleshooting
✅ Requires zero code changes for configuration updates
✅ Handles any JSON message structure flexibly

---

## System Architecture

### Components

**Parser Applications (3 Spring Boot apps):**
- Kafka Parser (this application)
- DB Parser
- Elasticsearch Parser

**Dashboard API (1 Spring Boot app):**
- Exposes APIs for frontend analytics

**Frontend (Nginx):**
- Static file serving

**Shared Infrastructure:**
- PostgreSQL (configuration storage)
- Kafka (message broker)
- Elasticsearch (data indexing & search)

### Expected Load (Testing Environment)
- **Parsers**: 50 msg/sec each × 3 = **150 msg/sec total**
- **Dashboard API**: 10 req/sec
- **Total Processing**: ~160 operations/sec

---

## Complete Application Flow

### Phase 1: Application Startup 🚀

```
1. Spring Boot Application Starts
         ↓
2. Connects to PostgreSQL Database
         ↓
3. DynamicKafkaConsumerManager initializes (@PostConstruct)
         ↓
4. Queries connections table:
   SELECT * FROM connections WHERE connectiontype = 'kafka'
         ↓
5. For each Kafka connection found:
   a. Parses the 'details' JSON column
   b. Extracts: kafkaBrokers, topic, consumerGroupId, securityProtocol
   c. Creates a Kafka consumer for that topic
   d. Starts the consumer
         ↓
6. Application ready - listening to multiple Kafka topics dynamically
```

**Example**: If 2 records in `connections` table:
- Connection 1: Listens to `api_audit_zak_logs`
- Connection 2: Listens to `customer_events_topic`
- Both consumers start automatically!

---

### Phase 2: Message Consumption 📨

```
Message Flow:

1. Message arrives on Kafka topic (e.g., api_audit_zak_logs)
         ↓
2. DynamicKafkaConsumerManager receives the message
         ↓
3. Extracts API Name from message using configured path
   Example: Reads 'api_name' field → "GRN_CREATE_RECEIPT"
         ↓
4. Enhances the message by adding:
   - connectionName: "Kafka Frontend" (from connections.connectionname)
   - extractedApiName: "GRN_CREATE_RECEIPT" (from message payload)
         ↓
5. Forwards enhanced message to raw-data-topic_kafka
         ↓
6. Passes enhanced message to DynamicMessageProcessor
```

**Enhanced Message Example:**
```json
{
  "log_id": "123-456",
  "api_name": "GRN_CREATE_RECEIPT",
  "request_id": "correlation-id-789",
  "timestamp": "2025-10-09T15:30:00",
  "log_type": "REQUEST",
  "metadata": {...},
  "payload": {...},
  "connectionName": "Kafka Frontend",
  "extractedApiName": "GRN_CREATE_RECEIPT"
}
```

---

### Phase 3: API Metadata Lookup 🔍

```
DynamicMessageProcessor receives enhanced message:

1. Extracts extractedApiName: "GRN_CREATE_RECEIPT"
         ↓
2. Queries api_metadata table:
   SELECT * FROM api_metadata 
   WHERE api_name = 'GRN_CREATE_RECEIPT'
         ↓
3. Gets unique_id from api_metadata (e.g., "a1b2c3d4-...")
         ↓
4. Queries api_metadata_field table:
   SELECT * FROM api_metadata_field 
   WHERE api_metadata_id = 'a1b2c3d4-...'
         ↓
5. Gets all field mappings (Mandatory + Custom)
   Example: 11 Mandatory fields + 5 Custom fields = 16 total
```

**Field Configuration Example:**

| Field | Path | KeyStatus | MessageType |
|-------|------|-----------|-------------|
| APIName | api_name | Mandatory | NULL |
| Host | metadata.client_ip | Mandatory | NULL |
| RequestTime | timestamp | Mandatory | REQUEST |
| ResponseTime | payload.processed_date | Mandatory | RESPONSE |
| supplier_code | payload.supplier_code | Custom | REQUEST |

---

### Phase 4: Field Extraction 📝

```
For each field in api_metadata_field:

1. Check key_status:
   - Mandatory → Direct mapping to Elasticsearch field
   - Custom → Add to CustomField array
         ↓
2. Check message_type:
   - If message_type = 'REQUEST' and current = REQUEST → Extract ✅
   - If message_type = 'RESPONSE' and current = RESPONSE → Extract ✅
   - If message_type = NULL → Extract from any message ✅
   - Otherwise → Skip ⏭️
         ↓
3. Use 'path' column to extract value from JSON:
   Example: path = "metadata.client_ip" → "0:0:0:0:0:0:0:1"
         ↓
4. Map to Elasticsearch field using 'field' column:
   Example: field = "Host" → parsedData.setHost("0:0:0:0:0:0:0:1")
```

**Extraction Example:**
```
Message: {"metadata": {"client_ip": "192.168.1.1"}}
Field Config: {field: "Host", path: "metadata.client_ip"}
Result: Host = "192.168.1.1" ✅
```

---

### Phase 5: Request/Response Correlation 🔗

#### Scenario A: REQUEST Arrives First

```
1. Message has log_type = "REQUEST"
         ↓
2. Extract Mandatory fields (message_type = 'REQUEST' or NULL)
   - APIName, CorrelationID, Host, RequestTime, ResourcePath, etc.
         ↓
3. Extract Custom fields (only from REQUEST)
   - supplier_code, grn_number, total_amount, etc.
         ↓
4. Create ParsedAuditData object
         ↓
5. Store in pendingRequests cache (key = correlation_id)
         ↓
6. Schedule timeout (1 minute)
         ↓
7. Wait for RESPONSE...
```

#### Scenario B: RESPONSE Arrives

```
1. Message has log_type = "RESPONSE"
         ↓
2. Look up correlation_id in pendingRequests cache
         ↓
3. If found (matching REQUEST exists):
   a. Extract RESPONSE fields (ResponseTime, Status, StatusCode)
   b. Merge with REQUEST data
   c. Set isComplete = true
   d. Remove from pending cache
   e. Index complete transaction to Elasticsearch ✅
         ↓
4. If not found (orphaned RESPONSE):
   a. Create ParsedAuditData with RESPONSE fields only
   b. Store in completedTransactions cache
   c. Wait 1 minute for REQUEST
   d. Index partial data if REQUEST never arrives
```

**Correlation Timeline Example:**
```
Time 0:00 - REQUEST arrives (correlation_id: "abc-123")
            Stores: {RequestTime: "10:00:00", RequestPayload: {...}}
            Status: Pending (waiting for RESPONSE)

Time 0:01 - RESPONSE arrives (correlation_id: "abc-123")
            Finds matching REQUEST ✅
            Merges: {
              RequestTime: "10:00:00",
              ResponseTime: "10:00:01",
              Status: "SUCCESS",
              StatusCode: 200
            }
            Indexes complete transaction to Elasticsearch
            Status: Complete ✅
```

---

### Phase 6: Elasticsearch Indexing 📊

```
Complete/Partial Data Ready:

1. ParsedAuditData object contains:
   - All extracted Mandatory fields
   - All extracted Custom fields (nested array)
   - Full request payload (original JSON)
   - Full response payload (original JSON)
   - Correlation metadata
         ↓
2. Convert to JSON using ObjectMapper (with JSR310 support)
         ↓
3. Send to Elasticsearch via REST API:
   PUT /my_smartlogger_index/_doc/{id}
         ↓
4. Elasticsearch stores the document (HTTP 201 Created)
         ↓
5. Document is searchable and available for dashboards
```

---

### Phase 7: Parallel Consumers 🔄

While main processing happens, parallel consumers provide visibility:

```
ApiAuditConsumer (listening to api_audit_zak_logs):
├─ Receives REQUEST/RESPONSE audit logs
├─ Prints formatted logs to console (debugging)
├─ Forwards to raw-data-topic_kafka
└─ Provides visibility and backup

RawDataConsumer (listening to raw-data-topic_kafka):
├─ Receives all enhanced messages
├─ Prints raw JSON to console (debugging)
└─ Provides debugging visibility
```

---

## Database Schema

### 1. `connections` Table

Stores Kafka connection details for dynamic consumer creation.

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

**Example Insert:**
```sql
INSERT INTO connections (connectionname, connectiontype, details)
VALUES (
    'Kafka Frontend',
    'kafka',
    '{"kafkaBrokers":["localhost:9092"],"topic":"api_audit_zak_logs","consumerGroupId":"local-dev-consumer","securityProtocol":"PLAINTEXT","userName":"","password":""}'
);
```

---

### 2. `api_metadata` Table

Stores API configuration and metadata.

```sql
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
```

**Example Insert:**
```sql
INSERT INTO api_metadata (unique_id, api_name, connection_name, resource_path, status, api_content_type)
VALUES (
    'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d'::uuid,
    'GRN_CREATE_RECEIPT',
    'Kafka Frontend',
    '/api/grn/create-receipt',
    'ACTIVE',
    'JSON'
);
```

---

### 3. `api_metadata_field` Table

Stores field-level mapping configuration.

```sql
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

**Field Types:**

| key_status | Purpose | Elasticsearch Mapping |
|------------|---------|----------------------|
| Mandatory | Core fields | Direct field mapping |
| Custom | Business fields | Nested array (key-value) |

**Message Type Rules:**

| message_type | Extraction Rule |
|--------------|-----------------|
| REQUEST | Only from REQUEST messages |
| RESPONSE | Only from RESPONSE messages |
| NULL | From both REQUEST and RESPONSE |

**Example Inserts:**
```sql
-- Mandatory field (from REQUEST only)
INSERT INTO api_metadata_field 
(field, path, key_status, message_type, api_metadata_id, datatype, content_type)
VALUES 
('RequestTime', 'timestamp', 'Mandatory', 'REQUEST', 'a1b2c3d4-...'::uuid, 'DateTime', 'JSON');

-- Mandatory field (from RESPONSE only)
INSERT INTO api_metadata_field 
(field, path, key_status, message_type, api_metadata_id, datatype, content_type)
VALUES 
('ResponseTime', 'payload.processed_date', 'Mandatory', 'RESPONSE', 'a1b2c3d4-...'::uuid, 'DateTime', 'JSON');

-- Mandatory field (from both)
INSERT INTO api_metadata_field 
(field, path, key_status, message_type, api_metadata_id, datatype, content_type)
VALUES 
('APIName', 'api_name', 'Mandatory', NULL, 'a1b2c3d4-...'::uuid, 'String', 'JSON');

-- Custom field (only from REQUEST)
INSERT INTO api_metadata_field 
(field, path, key_status, message_type, api_metadata_id, datatype, content_type)
VALUES 
('supplier_code', 'payload.supplier_code', 'Custom', 'REQUEST', 'a1b2c3d4-...'::uuid, 'String', 'JSON');
```

**Complete Field Mapping Example:**
```sql
-- All Mandatory fields for GRN_CREATE_RECEIPT API
INSERT INTO api_metadata_field (field, path, key_status, message_type, api_metadata_id, datatype, content_type, date_pattern_string)
VALUES 
('APIName', 'api_name', 'Mandatory', NULL, 'a1b2c3d4-...'::uuid, 'String', 'JSON', NULL),
('CorrelationID', 'request_id', 'Mandatory', NULL, 'a1b2c3d4-...'::uuid, 'String', 'JSON', NULL),
('Host', 'metadata.client_ip', 'Mandatory', NULL, 'a1b2c3d4-...'::uuid, 'String', 'JSON', NULL),
('ParentID', 'request_id', 'Mandatory', NULL, 'a1b2c3d4-...'::uuid, 'String', 'JSON', NULL),
('ResourcePath', 'metadata.endpoint', 'Mandatory', NULL, 'a1b2c3d4-...'::uuid, 'String', 'JSON', NULL),
('RequestTime', 'timestamp', 'Mandatory', 'REQUEST', 'a1b2c3d4-...'::uuid, 'DateTime', 'JSON', 'yyyy-MM-dd''T''HH:mm:ss'),
('ResponseTime', 'payload.processed_date', 'Mandatory', 'RESPONSE', 'a1b2c3d4-...'::uuid, 'DateTime', 'JSON', 'yyyy-MM-dd''T''HH:mm:ss'),
('Status', 'payload.status', 'Mandatory', 'RESPONSE', 'a1b2c3d4-...'::uuid, 'String', 'JSON', NULL),
('StatusCode', 'metadata.response_status', 'Mandatory', 'RESPONSE', 'a1b2c3d4-...'::uuid, 'Integer', 'JSON', NULL),
('TransactionID', 'request_id', 'Mandatory', NULL, 'a1b2c3d4-...'::uuid, 'String', 'JSON', NULL),
('UniqueTransactionID', 'request_id', 'Mandatory', NULL, 'a1b2c3d4-...'::uuid, 'String', 'JSON', NULL);

-- Custom fields (business-specific)
INSERT INTO api_metadata_field (field, path, key_status, message_type, api_metadata_id, datatype, content_type)
VALUES 
('supplier_code', 'payload.supplier_code', 'Custom', 'REQUEST', 'a1b2c3d4-...'::uuid, 'String', 'JSON'),
('grn_number', 'payload.grn_number', 'Custom', 'REQUEST', 'a1b2c3d4-...'::uuid, 'String', 'JSON'),
('total_amount', 'payload.total_amount', 'Custom', 'REQUEST', 'a1b2c3d4-...'::uuid, 'String', 'JSON'),
('currency', 'payload.currency', 'Custom', 'REQUEST', 'a1b2c3d4-...'::uuid, 'String', 'JSON');
```

---

## Elasticsearch Index Structure

### Index Name
`my_smartlogger_index`

### Create Index with Mappings

```bash
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
          "value": { "type": "text", "fields": {"keyword": {"type": "keyword"}} }
        }
      },
      "indexedAt": { "type": "date" },
      "isComplete": { "type": "boolean" }
    }
  }
}
'
```

### Document Structure

**Mandatory Fields (Direct Mapping):**

| Field | Type | Source |
|-------|------|--------|
| APIName | keyword | api_name |
| CorrelationID | keyword | request_id |
| Host | keyword | metadata.client_ip |
| ParentID | keyword | request_id |
| RequestPayload | text | Full request JSON |
| RequestTime | date | timestamp (REQUEST) |
| ResourcePath | keyword | metadata.endpoint |
| ResponsePayload | text | Full response JSON |
| ResponseTime | date | payload.processed_date (RESPONSE) |
| Status | keyword | payload.status |
| StatusCode | integer | metadata.response_status |
| TransactionID | keyword | request_id |
| UniqueTransactionID | keyword | request_id |

**Custom Fields (Nested Array):**
```json
"CustomField": [
  {"key": "supplier_code", "value": "SUP001"},
  {"key": "grn_number", "value": "GRN-2024-001"},
  {"key": "total_amount", "value": "1000.0"},
  {"key": "currency", "value": "USD"}
]
```

**Metadata Fields:**

| Field | Type | Purpose |
|-------|------|---------|
| indexedAt | date | Document creation timestamp |
| isComplete | boolean | true if REQUEST + RESPONSE correlated |

---

## End-to-End Example

### Scenario: Processing a GRN Receipt Transaction

#### Step 1: Message Arrives on Kafka

**REQUEST Message:**
```json
{
  "log_id": "req-log-123",
  "api_name": "GRN_CREATE_RECEIPT",
  "request_id": "txn-789",
  "timestamp": "2025-10-09T10:00:00",
  "log_type": "REQUEST",
  "metadata": {
    "client_ip": "192.168.1.100",
    "http_method": "POST",
    "endpoint": "/api/grn/create-receipt"
  },
  "payload": {
    "grn_number": "GRN-2024-001",
    "supplier_code": "SUP001",
    "total_amount": 1000.0,
    "currency": "USD"
  }
}
```

#### Step 2: Enhancement

```json
{
  ...original fields...,
  "connectionName": "Kafka Frontend",
  "extractedApiName": "GRN_CREATE_RECEIPT"
}
```

#### Step 3: Database Queries

**Query 1:**
```sql
SELECT unique_id FROM api_metadata 
WHERE api_name = 'GRN_CREATE_RECEIPT';
-- Result: 'a1b2c3d4-...'
```

**Query 2:**
```sql
SELECT * FROM api_metadata_field 
WHERE api_metadata_id = 'a1b2c3d4-...';
-- Result: 16 fields (11 Mandatory, 5 Custom)
```

#### Step 4: REQUEST Processing

**Mandatory Fields Extracted:**
- APIName: "GRN_CREATE_RECEIPT"
- CorrelationID: "txn-789"
- Host: "192.168.1.100"
- RequestTime: "2025-10-09T10:00:00"
- ResourcePath: "/api/grn/create-receipt"

**Custom Fields Extracted:**
- supplier_code: "SUP001"
- grn_number: "GRN-2024-001"
- total_amount: "1000.0"
- currency: "USD"

**Status**: Stored in pending cache, waiting for RESPONSE...

#### Step 5: RESPONSE Arrives (1 second later)

**RESPONSE Message:**
```json
{
  "log_id": "resp-log-456",
  "api_name": "GRN_CREATE_RECEIPT",
  "request_id": "txn-789",
  "timestamp": "2025-10-09T10:00:01",
  "log_type": "RESPONSE",
  "metadata": {
    "client_ip": "192.168.1.100",
    "response_status": 200,
    "processing_time_ms": 150
  },
  "payload": {
    "status": "SUCCESS",
    "message": "GRN created successfully",
    "processed_date": "2025-10-09T10:00:01"
  }
}
```

#### Step 6: Correlation & Merge

```
1. Finds matching REQUEST (correlation_id = "txn-789") ✅
2. Extracts RESPONSE fields:
   - ResponseTime: "2025-10-09T10:00:01"
   - Status: "SUCCESS"
   - StatusCode: 200
3. Merges with REQUEST data
4. Sets isComplete = true
```

#### Step 7: Final Elasticsearch Document

```json
{
  "id": "es-doc-uuid-123",
  "APIName": "GRN_CREATE_RECEIPT",
  "CorrelationID": "txn-789",
  "Host": "192.168.1.100",
  "ParentID": "txn-789",
  "RequestPayload": "{\"grn_number\":\"GRN-2024-001\",\"supplier_code\":\"SUP001\",\"total_amount\":1000.0,\"currency\":\"USD\"}",
  "RequestTime": "2025-10-09T10:00:00",
  "ResourcePath": "/api/grn/create-receipt",
  "ResponsePayload": "{\"status\":\"SUCCESS\",\"message\":\"GRN created successfully\",\"processed_date\":\"2025-10-09T10:00:01\"}",
  "ResponseTime": "2025-10-09T10:00:01",
  "Status": "SUCCESS",
  "StatusCode": 200,
  "TransactionID": "txn-789",
  "UniqueTransactionID": "txn-789",
  "CustomField": [
    {"key": "supplier_code", "value": "SUP001"},
    {"key": "grn_number", "value": "GRN-2024-001"},
    {"key": "total_amount", "value": "1000.0"},
    {"key": "currency", "value": "USD"}
  ],
  "indexedAt": "2025-10-09T10:00:01.500",
  "isComplete": true
}
```

**Status**: HTTP 201 Created ✅

---

## Use Cases

### Use Case 1: API Performance Monitoring

**Question**: Which APIs have the slowest response times?

**Elasticsearch Query:**
```json
{
  "size": 0,
  "aggs": {
    "by_api": {
      "terms": {"field": "APIName"},
      "aggs": {
        "avg_response_time": {
          "avg": {
            "script": "doc['ResponseTime'].value.toInstant().toEpochMilli() - doc['RequestTime'].value.toInstant().toEpochMilli()"
          }
        }
      }
    }
  }
}
```

**Sample Result:**
- GRN_CREATE_RECEIPT: avg 250ms
- ORDER_CREATE: avg 1.2s
- PAYMENT_PROCESS: avg 3.5s

**Action**: Investigate PAYMENT_PROCESS for optimization opportunities

---

### Use Case 2: Error Rate Analysis

**Question**: How many failed requests in the last hour?

**Elasticsearch Query:**
```json
{
  "query": {
    "bool": {
      "must": [
        {"range": {"RequestTime": {"gte": "now-1h"}}},
        {"bool": {"must_not": {"term": {"Status": "SUCCESS"}}}}
      ]
    }
  }
}
```

**Sample Result:**
- 15 failures in last hour
- 10 from PAYMENT_PROCESS API
- 5 from ORDER_CREATE API

**Action**: Alert dev team about PAYMENT_PROCESS issues

---

### Use Case 3: Business Analytics

**Question**: What's the total transaction amount by supplier today?

**Elasticsearch Query:**
```json
{
  "query": {
    "bool": {
      "must": [
        {"term": {"APIName": "GRN_CREATE_RECEIPT"}},
        {"range": {"RequestTime": {"gte": "now/d"}}}
      ]
    }
  },
  "aggs": {
    "custom_fields": {
      "nested": {"path": "CustomField"},
      "aggs": {
        "amounts": {
          "filter": {"term": {"CustomField.key": "total_amount"}},
          "aggs": {
            "total": {
              "sum": {
                "field": "CustomField.value.keyword",
                "script": "Double.parseDouble(_value)"
              }
            }
          }
        }
      }
    }
  }
}
```

**Sample Result:**
- Total amount processed today: $145,231.50
- Number of transactions: 1,452

---

### Use Case 4: Transaction Troubleshooting

**Question**: What happened with transaction "txn-789"?

**Elasticsearch Query:**
```bash
curl -X GET "http://localhost:9200/my_smartlogger_index/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {"term": {"CorrelationID": "txn-789"}}
}'
```

**Result:**
Complete audit trail including:
- Full request payload
- Full response payload
- Exact request/response timestamps
- Status and status code
- All custom business fields
- Processing time
- Client IP and metadata
- Source connection name

---

## Hardware Requirements

### Testing Environment (50 msg/sec per parser)

**Single Server Configuration:**

```
Specifications:
├─ CPU: 8-12 cores
├─ RAM: 24-32 GB
├─ Disk: 500 GB SSD
└─ Network: 1 Gbps

Resource Allocation:
├─ 3 Parser Apps: 3 cores, 3 GB RAM (1 core, 1 GB each)
├─ Dashboard API: 1 core, 1 GB RAM
├─ Nginx: 1 core, 512 MB RAM
├─ PostgreSQL: 1 core, 2 GB RAM
├─ Kafka: 2 cores, 4 GB RAM
├─ Elasticsearch: 4 cores, 12 GB RAM
└─ OS + Overhead: 2 cores, 6 GB RAM

Cloud Cost: ~$150-250/month
On-Premise: ~$1,500-2,500 (one-time)
```

### Storage Estimates (7-day retention)

**Kafka:**
- 150 msg/sec × 2 KB × 86,400 sec/day = 26 GB/day
- 7-day retention = **~180 GB**

**Elasticsearch:**
- 75 msg/sec (after correlation) × 3 KB × 86,400 sec/day = 19.5 GB/day
- 7-day retention = **~140 GB**
- With replication factor = **~280 GB**

**PostgreSQL:**
- Metadata tables: **< 5 GB**

**Total: 500 GB SSD sufficient**

---

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
# Copy the example config
cp config.env.example config.env

# Edit with your actual values
notepad config.env  # Windows
nano config.env     # Linux/Mac
```

#### 2. Build and start:
```bash
docker-compose up -d
```

#### 3. Check status:
```bash
docker-compose ps
```

#### 4. View logs:
```bash
docker-compose logs -f app
```

#### 5. Stop application:
```bash
docker-compose down
```

### Configuration File (config.env)

```env
# Database Configuration
DB_HOST=host.docker.internal
DB_PORT=5432
DB_NAME=businessinsight
DB_USERNAME=audituser
DB_PASSWORD=your_password

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092
KAFKA_CONSUMER_GROUP=kafka-parser-consumer-group

# Elasticsearch Configuration
ELASTICSEARCH_HOST=host.docker.internal
ELASTICSEARCH_PORT=9200

# Application Configuration
SERVER_PORT=8080

# Audit Processor Configuration
AUDIT_TIMEOUT_MINUTES=1
AUDIT_CLEANUP_INTERVAL_MINUTES=5
```

**Note:** `host.docker.internal` works on Docker Desktop (Windows/Mac). On Linux, use your actual host IP address.

---

## Configuration Management

### Adding a New Kafka Connection

```sql
-- 1. Insert into connections table
INSERT INTO connections (connectionname, connectiontype, details)
VALUES (
    'Customer Events Source',
    'kafka',
    '{"kafkaBrokers":["kafka-server:9092"],"topic":"customer_events","consumerGroupId":"customer-consumer","securityProtocol":"PLAINTEXT"}'
);

-- 2. Restart application
-- New consumer will automatically start listening to 'customer_events' topic
```

### Adding a New API

```sql
-- 1. Add API metadata
INSERT INTO api_metadata (unique_id, api_name, connection_name, resource_path, status)
VALUES (
    gen_random_uuid(),
    'ORDER_CREATE',
    'Kafka Frontend',
    '/api/order/create',
    'ACTIVE'
);

-- 2. Get the generated unique_id
SELECT unique_id FROM api_metadata WHERE api_name = 'ORDER_CREATE';
-- Example result: 'b2c3d4e5-f6a7-5b6c-9d0e-1f2a3b4c5d6e'

-- 3. Add field mappings
INSERT INTO api_metadata_field 
(field, path, key_status, message_type, api_metadata_id, datatype)
VALUES 
('APIName', 'api_name', 'Mandatory', NULL, 'b2c3d4e5-...'::uuid, 'String'),
('CorrelationID', 'request_id', 'Mandatory', NULL, 'b2c3d4e5-...'::uuid, 'String'),
('RequestTime', 'timestamp', 'Mandatory', 'REQUEST', 'b2c3d4e5-...'::uuid, 'DateTime'),
('ResponseTime', 'payload.timestamp', 'Mandatory', 'RESPONSE', 'b2c3d4e5-...'::uuid, 'DateTime'),
('order_number', 'payload.order_number', 'Custom', 'REQUEST', 'b2c3d4e5-...'::uuid, 'String'),
('customer_id', 'payload.customer_id', 'Custom', 'REQUEST', 'b2c3d4e5-...'::uuid, 'String');

-- 4. No restart needed - next message will use new configuration
```

### Updating Field Paths

```sql
-- Fix incorrect path
UPDATE api_metadata_field 
SET path = 'metadata.client_ip'
WHERE field = 'Host' 
  AND path = 'metadata/client_ip'  -- Old incorrect path with slash
  AND api_metadata_id = 'a1b2c3d4-...'::uuid;

-- Update date pattern
UPDATE api_metadata_field 
SET date_pattern_string = 'yyyy-MM-dd''T''HH:mm:ss.SSS'
WHERE field = 'ResponseTime'
  AND api_metadata_id = 'a1b2c3d4-...'::uuid;
```

---

## API Endpoints

### Audit Processor Monitoring

**GET `/api/audit-processor/stats`**

Returns statistics about message processing.

**Response:**
```json
{
  "status": "success",
  "statistics": "DynamicMessageProcessor Stats - Pending: 5, Completed: 120, Timeout: 1 min",
  "message": "Audit processor is running"
}
```

**GET `/api/audit-processor/health`**

Health check endpoint.

**Response:**
```json
{
  "status": "success",
  "message": "Dynamic Message Processor is running!",
  "endpoints": [
    "GET /api/audit-processor/stats - Get processor statistics",
    "GET /api/audit-processor/health - Health check"
  ]
}
```

---

## Troubleshooting

### Issue 1: No Data in Elasticsearch

**Symptoms**: Index is empty despite Kafka messages

**Debug Steps:**

1. **Check if consumers started:**
```
Look for log: "Creating consumers for connection: ..."
```

2. **Check if API name extracted:**
```
Look for log: "Extracted API Name: ... from connection: ..."
If missing: Check connections table 'details' JSON has correct API name path
```

3. **Check if API metadata found:**
```
Look for log: "Field configuration loaded for API '...': X total fields"
If missing: Add API to api_metadata and api_metadata_field tables
```

4. **Check field extraction:**
```
Look for log: "Extracted FieldName: path = value"
If fields not found: Verify JSON paths use dot notation (metadata.field)
```

5. **Check Elasticsearch indexing:**
```
Look for log: "Successfully indexed audit data to my_smartlogger_index"
If missing: Check Elasticsearch connection and index exists
```

---

### Issue 2: isComplete Always False

**Symptoms**: All documents have `isComplete: false`

**Possible Causes:**

1. **No RESPONSE messages arriving**
   - Check if RESPONSE messages sent to Kafka
   - Verify log_type field is "RESPONSE"

2. **Correlation ID mismatch**
   - Ensure REQUEST and RESPONSE have same request_id
   - Check logs for correlation_id values

3. **Timeout too short**
   - Increase AUDIT_TIMEOUT_MINUTES in config.env
   - Default is 1 minute

4. **RESPONSE arrives before REQUEST**
   - System handles this (orphaned response)
   - Check logs for "Orphaned RESPONSE detected"

---

### Issue 3: Custom Fields Empty

**Symptoms**: `CustomField` array is empty or missing

**Debug Steps:**

1. **Check key_status in database:**
```sql
SELECT field, key_status, message_type 
FROM api_metadata_field 
WHERE api_metadata_id = 'your-uuid';
```

2. **Verify message_type:**
   - Custom fields must have message_type = 'REQUEST'
   - Or set to NULL if extracting from both

3. **Check JSON paths:**
   - Must use dot notation: `payload.supplier_code`
   - Not slash notation: `payload/supplier_code`

4. **Verify field exists in message:**
```
Look for log: "Custom Field - fieldname: path = value"
If "not found": Field doesn't exist at that path in message
```

---

### Issue 4: Date/Time Fields Are Null

**Symptoms**: RequestTime or ResponseTime is null

**Debug Steps:**

1. **Check date pattern:**
```sql
SELECT field, path, date_pattern_string, message_type
FROM api_metadata_field 
WHERE field IN ('RequestTime', 'ResponseTime');
```

2. **Verify pattern matches actual format:**
   - Database: `yyyy-MM-dd'T'HH:mm:ss`
   - Message: `2025-10-09T10:00:00` ✅

3. **Check message_type:**
   - RequestTime → message_type = 'REQUEST'
   - ResponseTime → message_type = 'RESPONSE'

4. **Look for parsing errors in logs:**
```
"Failed to parse date time '...' with pattern '...'"
```

---

### Issue 5: Duplicate Field Extraction

**Symptoms**: Same field extracted multiple times

**Cause**: Multiple records in `api_metadata_field` with same field name

**Fix:**
```sql
-- Find duplicates
SELECT field, COUNT(*) 
FROM api_metadata_field 
WHERE api_metadata_id = 'your-uuid'
GROUP BY field 
HAVING COUNT(*) > 1;

-- Remove duplicates (keep one, delete others)
DELETE FROM api_metadata_field 
WHERE id = duplicate_id;
```

---

## Error Handling & Edge Cases

### Case 1: Missing Fields in Message

```
Configured: Extract "Host" from "metadata.client_ip"
Message: {"metadata": {}}  ← No client_ip field
Behavior: Field skipped, continues processing
Result: Document indexed with Host = null
Log: "⚠️ Field Host not found at path: metadata.client_ip"
```

### Case 2: No RESPONSE Within Timeout

```
Time 0:00 - REQUEST arrives (correlation_id: "abc-123")
Time 0:30 - Still waiting...
Time 1:00 - Timeout! (AUDIT_TIMEOUT_MINUTES = 1)
Behavior: Indexes REQUEST data only
Result: Document with isComplete = false, no ResponseTime/Status/StatusCode
Log: "⏰ Timeout reached for REQUEST: abc-123. Indexing with available data."
```

### Case 3: Orphaned RESPONSE

```
Time 0:00 - RESPONSE arrives (correlation_id: "xyz-456")
            No matching REQUEST found
Behavior: Stores in completedTransactions cache, waits 1 minute
Time 0:30 - REQUEST arrives (correlation_id: "xyz-456")
Behavior: Matches with orphaned RESPONSE, merges, indexes ✅
Log: "🔄 Orphaned RESPONSE detected for ID: xyz-456. Waiting for REQUEST..."
     "✅ Transaction completed for ID: xyz-456"
```

### Case 4: Invalid Date Format

```
Configured: date_pattern_string = 'yyyy-MM-dd HH:mm:ss'
Message: timestamp = "2025-10-09T10:00:00"  ← Format mismatch
Behavior: Parsing fails, field set to null
Result: RequestTime/ResponseTime = null
Log: "⚠️ Failed to parse date time '2025-10-09T10:00:00' with pattern 'yyyy-MM-dd HH:mm:ss'"
Fix: Update date_pattern_string to 'yyyy-MM-dd''T''HH:mm:ss'
```

### Case 5: No API Configuration

```
Message: extractedApiName = "UNKNOWN_API"
Database: No record in api_metadata for "UNKNOWN_API"
Behavior: Warning logged, processing stopped for this message
Result: Not indexed to Elasticsearch
Log: "⚠️ No field configuration found for API: UNKNOWN_API"
Fix: Add API to api_metadata and api_metadata_field tables
```

---

## Monitoring & Observability

### Application Logs - What to Watch

**Startup Logs:**
```
✅ "Initializing dynamic Kafka consumers from database..."
✅ "Loaded X Kafka connections from database"
✅ "Creating consumers for connection: ... (ID: ...)"
✅ "Created X consumers for connection '...'"
✅ "Dynamic Kafka consumer initialization complete"
```

**Message Processing Logs:**
```
✅ "Received message from topic '...' (connection: ...)"
✅ "Extracted API Name: ... from connection: ..."
✅ "Field configuration loaded for API '...': X total fields"
✅ "Processing REQUEST - ID: ..., API: ..., Connection: ..."
✅ "Extracting X mandatory fields from REQUEST message"
✅ "Extracted X Mandatory fields and Y Custom fields"
✅ "REQUEST stored, waiting for RESPONSE. Pending requests: X"
✅ "Processing RESPONSE - ID: ..., API: ..., Connection: ..."
✅ "Transaction completed for ID: ..."
✅ "Successfully indexed audit data to my_smartlogger_index"
```

**Warning Logs:**
```
⚠️ "No extractedApiName found in message from connection: ..."
⚠️ "No field configuration found for API: ..."
⚠️ "Field X not found at path: ..."
⚠️ "Orphaned RESPONSE detected for ID: ..."
⚠️ "Timeout reached for REQUEST: ..."
```

**Error Logs:**
```
❌ "Failed to create consumer for topic '...' on connection '...'"
❌ "Error processing dynamic message from connection ..."
❌ "Failed to index audit data to my_smartlogger_index"
```

### Key Metrics

**Processor Statistics:**
```bash
curl http://localhost:8080/api/audit-processor/stats
```
Returns: `Pending: X, Completed: Y, Timeout: Z min`

**Elasticsearch Document Count:**
```bash
curl http://localhost:9200/my_smartlogger_index/_count
```

**Correlation Success Rate:**
```bash
# Complete transactions
curl -X GET "http://localhost:9200/my_smartlogger_index/_count" -d'
{"query": {"term": {"isComplete": true}}}'

# Total transactions
curl http://localhost:9200/my_smartlogger_index/_count

# Success Rate = Complete / Total × 100%
```

---

## Technology Stack

### Backend Framework
- **Spring Boot**: 2.7.18
- **Java**: 11
- **Maven**: 3.8+

### Database
- **PostgreSQL**: 14+ (configuration storage)
- **Spring Data JPA**: 2.7.18
- **Hibernate**: ORM layer

### Messaging
- **Apache Kafka**: 2.8+
- **Spring Kafka**: 2.8.11

### Search & Analytics
- **Elasticsearch**: 7.17.9
- **Elasticsearch REST Client**: 7.17.9
- **Spring Data Elasticsearch**: 2.7.18

### Serialization
- **Jackson Core**: 2.13.5
- **Jackson Datatype JSR310**: 2.13.5 (Java 8 time support)
- **Jackson XML**: 2.13.5

### Containerization
- **Docker**: 20.x+
- **Docker Compose**: 2.x+

---

## File Structure

```
KafkaParsing/
├── src/main/java/com/example/kafkaparsing/
│   ├── config/
│   │   ├── ElasticsearchConfig.java       # Elasticsearch client setup
│   │   ├── KafkaConfig.java                # Kafka producer config
│   │   └── KafkaConsumerConfig.java        # Kafka consumer factory
│   ├── consumer/
│   │   ├── ApiAuditConsumer.java           # Forwards api_audit_zak_logs → raw-data-topic_kafka
│   │   └── RawDataConsumer.java            # Logs messages from raw-data-topic_kafka
│   ├── controller/
│   │   └── AuditProcessorController.java   # Monitoring endpoints
│   ├── entity/
│   │   ├── ApiMetadata.java                # api_metadata table entity
│   │   ├── ApiMetadataField.java           # api_metadata_field table entity
│   │   └── DataSourceConnection.java       # connections table entity
│   ├── model/
│   │   ├── ApiAuditLog.java                # Kafka audit log structure
│   │   ├── ApiRequestMetadata.java         # HTTP request/response metadata
│   │   ├── KafkaConnectionDetails.java     # Kafka connection config
│   │   ├── ParsedAuditData.java            # Elasticsearch document model
│   │   └── RetryConfig.java                # Retry configuration
│   ├── repository/
│   │   ├── ApiMetadataFieldRepository.java # Field config queries
│   │   ├── ApiMetadataRepository.java      # API metadata queries
│   │   └── DataSourceConnectionRepository.java # Connection queries
│   ├── service/
│   │   ├── ApiMetadataService.java         # Load field configuration
│   │   ├── DataSourceConnectionService.java # Load Kafka connections
│   │   ├── DynamicKafkaConsumerManager.java # Consumer lifecycle management
│   │   ├── DynamicMessageProcessor.java    # Message processing & correlation
│   │   ├── ElasticsearchService.java       # Elasticsearch operations
│   │   └── KafkaMessageForwarder.java      # Message forwarding with retry
│   └── KafkaParsingApplication.java        # Main application class
├── src/main/resources/
│   └── application.yml                     # Spring Boot configuration
├── Dockerfile                              # Container build instructions
├── docker-compose.yml                      # Container orchestration
├── config.env.example                      # Configuration template
├── .dockerignore                           # Docker build exclusions
├── .gitignore                              # Git exclusions
├── pom.xml                                 # Maven dependencies
└── README.md                               # This file
```

---

## Key Advantages

### 1. Configuration-Driven Architecture

**No Code Changes Needed For:**
- ✅ Adding new Kafka topics → Insert into `connections` table
- ✅ Adding new APIs → Insert into `api_metadata` table
- ✅ Modifying field mappings → Update `api_metadata_field` table
- ✅ Changing extraction paths → Update `path` column
- ✅ Adding custom fields → Insert new rows with key_status = 'Custom'

### 2. Flexible Message Handling

- ✅ Any JSON structure supported (via JSON path)
- ✅ Missing fields handled gracefully (no errors, continues processing)
- ✅ Context-aware extraction (REQUEST vs RESPONSE vs both)
- ✅ Custom date patterns per field
- ✅ Nested field access (e.g., `metadata.client_ip`, `payload.order.total`)

### 3. Intelligent Correlation

- ✅ Automatic REQUEST + RESPONSE matching by correlation ID
- ✅ Configurable timeout (default 1 minute)
- ✅ Handles orphaned messages (RESPONSE before REQUEST)
- ✅ Indexes partial data if correlation fails
- ✅ Async processing for performance

### 4. Multi-Source Support

- ✅ Unlimited Kafka topics (limited only by database records)
- ✅ Different configurations per connection
- ✅ Connection tracking (knows source of each message)
- ✅ Secure credential management per connection

### 5. Business Intelligence Ready

- ✅ Standard fields for technical monitoring
- ✅ Custom fields for business analytics
- ✅ Full payloads for deep investigation
- ✅ Timestamp-based retention and queries
- ✅ Nested custom fields for complex analysis

---

## Common Elasticsearch Queries

### Get Document Count
```bash
curl -X GET "http://localhost:9200/my_smartlogger_index/_count?pretty"
```

### Get All Documents
```bash
curl -X GET "http://localhost:9200/my_smartlogger_index/_search?pretty&size=10"
```

### Search by API Name
```bash
curl -X GET "http://localhost:9200/my_smartlogger_index/_search?pretty" \
-H 'Content-Type: application/json' -d'
{
  "query": {
    "term": {"APIName": "GRN_CREATE_RECEIPT"}
  }
}'
```

### Search Complete Transactions Only
```bash
curl -X GET "http://localhost:9200/my_smartlogger_index/_search?pretty" \
-H 'Content-Type: application/json' -d'
{
  "query": {
    "term": {"isComplete": true}
  }
}'
```

### Search by Correlation ID
```bash
curl -X GET "http://localhost:9200/my_smartlogger_index/_search?pretty" \
-H 'Content-Type: application/json' -d'
{
  "query": {
    "term": {"CorrelationID.keyword": "your-correlation-id"}
  }
}'
```

### Search Failed Transactions
```bash
curl -X GET "http://localhost:9200/my_smartlogger_index/_search?pretty" \
-H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must_not": {
        "term": {"Status": "SUCCESS"}
      }
    }
  }
}'
```

### Search by Custom Field
```bash
curl -X GET "http://localhost:9200/my_smartlogger_index/_search?pretty" \
-H 'Content-Type: application/json' -d'
{
  "query": {
    "nested": {
      "path": "CustomField",
      "query": {
        "bool": {
          "must": [
            {"term": {"CustomField.key": "supplier_code"}},
            {"term": {"CustomField.value.keyword": "SUP001"}}
          ]
        }
      }
    }
  }
}'
```

### Get Recent Documents (Last Hour)
```bash
curl -X GET "http://localhost:9200/my_smartlogger_index/_search?pretty" \
-H 'Content-Type: application/json' -d'
{
  "query": {
    "range": {
      "indexedAt": {"gte": "now-1h"}
    }
  },
  "sort": [{"indexedAt": {"order": "desc"}}]
}'
```

---

## Performance Tuning

### Application Level

**JVM Tuning (config.env):**
```env
JAVA_OPTS=-Xmx1536m -Xms1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

**Timeout Tuning (application.yml):**
```yaml
audit:
  processor:
    timeout-minutes: 2  # Increase if responses take longer
    cleanup-interval-minutes: 10  # Reduce for less frequent cleanup
```

**Thread Pool (for high concurrency):**
```java
// In DynamicMessageProcessor
private final ExecutorService executorService = Executors.newFixedThreadPool(10);
// Increase from 5 to 10 for more concurrent timeout processing
```

### Kafka Tuning

**Consumer Configuration (application.yml):**
```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 500  # Batch size
      fetch-min-size: 1024   # Minimum fetch size
      fetch-max-wait: 500    # Max wait time
```

### Elasticsearch Tuning

**Bulk Indexing** (for future optimization):
- Batch multiple documents together
- Reduces network overhead
- Improves throughput

**Refresh Interval:**
```bash
curl -X PUT "http://localhost:9200/my_smartlogger_index/_settings" -d'
{
  "index": {
    "refresh_interval": "30s"
  }
}'
```

---

## Security Considerations

### Configuration Security

**DO:**
- ✅ Keep `config.env` out of git (already in .gitignore)
- ✅ Use environment-specific config files
- ✅ Rotate passwords regularly
- ✅ Use secrets management in production (AWS Secrets, Vault)

**DON'T:**
- ❌ Commit `config.env` with real passwords
- ❌ Hardcode credentials in code
- ❌ Share config files via email/chat

### Database Security

```sql
-- Use read-only user for application
CREATE USER kafka_parser_ro WITH PASSWORD 'secure_password';
GRANT SELECT ON connections, api_metadata, api_metadata_field TO kafka_parser_ro;
```

### Kafka Security

For production, configure SASL/SSL in `connections.details`:
```json
{
  "securityProtocol": "SASL_SSL",
  "userName": "kafka_user",
  "password": "secure_password",
  "certificate": "base64_encoded_cert"
}
```

---

## Scaling Strategy

### Horizontal Scaling (Add More Instances)

**For Higher Load:**
```yaml
# docker-compose.yml
services:
  kafka-parser-1:
    # ... config ...
    container_name: kafka-parser-1
  
  kafka-parser-2:
    # ... config ...
    container_name: kafka-parser-2
    
  kafka-parser-3:
    # ... config ...
    container_name: kafka-parser-3
```

**Important**: Use different consumer group IDs to avoid conflicts

### Vertical Scaling (More Resources)

```yaml
# docker-compose.yml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 4G
```

### Elasticsearch Scaling

**Add More Nodes:**
- 3-node cluster for high availability
- Distribute shards across nodes
- Enable replication for fault tolerance

---

## Maintenance

### Regular Tasks

**Daily:**
- Monitor pending request count
- Check error logs
- Verify Elasticsearch indexing rate

**Weekly:**
- Review correlation success rate
- Check disk usage trends
- Analyze slow queries

**Monthly:**
- Archive old Elasticsearch indices
- Review and optimize field mappings
- Update dependencies

### Backup Strategy

**PostgreSQL:**
```bash
pg_dump -U audituser businessinsight > backup_$(date +%Y%m%d).sql
```

**Elasticsearch:**
```bash
# Create snapshot repository
curl -X PUT "http://localhost:9200/_snapshot/my_backup" -d'
{
  "type": "fs",
  "settings": {
    "location": "/backup/elasticsearch"
  }
}'

# Create snapshot
curl -X PUT "http://localhost:9200/_snapshot/my_backup/snapshot_1?wait_for_completion=true"
```

---

## Development

### Local Development Setup

```bash
# 1. Clone repository
git clone https://github.com/zaklub/SmaerLoggerKafkaParser.git
cd SmaerLoggerKafkaParser

# 2. Configure application
# Edit src/main/resources/application.yml with local settings

# 3. Run with Maven
.\mvnw.cmd spring-boot:run  # Windows
./mvnw spring-boot:run      # Linux/Mac

# 4. Application starts on http://localhost:8080
```

### Building JAR

```bash
# Clean build
.\mvnw.cmd clean package

# Skip tests
.\mvnw.cmd clean package -DskipTests

# Output: target/kafka-parsing-1.0.0.jar
```

### Running Tests

```bash
.\mvnw.cmd test
```

---

## Production Deployment Checklist

- [ ] Database tables created (connections, api_metadata, api_metadata_field)
- [ ] Kafka connections configured in database
- [ ] API metadata and field mappings configured
- [ ] Elasticsearch index created with proper mappings
- [ ] config.env file created with production credentials
- [ ] JVM memory settings optimized
- [ ] Logging configured appropriately
- [ ] Monitoring dashboard set up
- [ ] Alerts configured for errors and high pending counts
- [ ] Backup strategy implemented
- [ ] Disaster recovery plan documented
- [ ] Security review completed
- [ ] Load testing performed

---

## FAQ

### Q: How do I add a new Kafka topic to monitor?
**A**: Insert a new record in the `connections` table with the topic details. Restart the application. The new consumer will automatically be created.

### Q: Can I change field mappings without redeploying?
**A**: Yes! Update the `api_metadata_field` table. The next message processed will use the new configuration (no restart needed for new messages, but existing pending correlations use old config).

### Q: What happens if Elasticsearch is down?
**A**: The application logs errors but continues processing. Messages are lost for that period. Consider implementing a dead letter queue for production.

### Q: How long are messages kept in the pending cache?
**A**: Configurable via `AUDIT_TIMEOUT_MINUTES` (default 1 minute). After timeout, partial data is indexed.

### Q: Can I process messages without correlation (single messages)?
**A**: Yes! If `log_type` is not "REQUEST" or "RESPONSE", or if no matching message is found within timeout, single messages are indexed.

### Q: How do I extract nested JSON fields?
**A**: Use dot notation in the `path` column. Example: `payload.order.items[0].price` → Use `payload.order.items` (array indexing not supported, extracts first element by default).

### Q: Can I have different timeout values for different APIs?
**A**: Currently, timeout is global. For API-specific timeouts, you'd need to extend the code.

### Q: What's the maximum message size supported?
**A**: Limited by Kafka's `max.message.bytes` (default 1 MB). For larger messages, increase Kafka configuration.

---

## License

This project is licensed under the MIT License.

---

## Support & Contact

For issues, questions, or contributions:
- **Repository**: https://github.com/zaklub/SmaerLoggerKafkaParser
- **Issues**: Create a GitHub issue
- **Documentation**: This README

---

## Changelog

### Version 1.0.0 (Current)
- ✅ Dynamic Kafka consumer management from database
- ✅ Database-driven field mapping (Mandatory + Custom fields)
- ✅ Request/Response correlation with configurable timeout
- ✅ Elasticsearch indexing with nested custom fields
- ✅ Message-type aware field extraction
- ✅ Connection name and API name tracking
- ✅ Docker containerization
- ✅ Comprehensive error handling
- ✅ JSON path-based dynamic field extraction

---

**Built with ❤️ using Spring Boot, Kafka, and Elasticsearch**
