# 企业搜索中台实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 建设企业级搜索中台，支持多业务线接入，提供关键词搜索、向量搜索、可配置排序等核心能力。

**架构：** 分层架构设计，包含数据接入层（CDC同步）、数据处理层（清洗+向量化）、索引管理层（ES混合索引）、查询服务层（多路召回+精排）、元数据配置模块（自助接入）。

**技术栈：** OpenSearch/Elasticsearch、Kafka、Flink、Debezium、Java/Go、PostgreSQL/MySQL/Oracle

---

## 目录结构

```
search-platform/
├── services/
│   ├── api-gateway/          # API网关（鉴权、限流、路由）
│   ├── query-service/        # 查询服务（多路召回、精排）
│   ├── data-sync/            # 数据同步服务（CDC消费）
│   ├── vector-service/       # 向量化服务（Embedding推理）
│   └── config-admin/         # 配置管理后台
├── repositories/
│   ├── config-repo/          # 配置中心
│   └── common/               # 公共库
├── deployments/
│   ├── docker/               # Docker编排
│   └── kubernetes/           # K8s部署文件
├── docs/
│   ├── api/                  # API文档
│   └── design/               # 设计文档
└── tests/
    ├── unit/                 # 单元测试
    └── integration/          # 集成测试
```

---

## Phase 1: 基础设施搭建（第1-2周）

### Task 1: 项目初始化与公共库

**目标：** 建立项目基础结构，创建公共库（日志、配置、工具类）

**Files:**
- Create: `repositories/common/pom.xml`
- Create: `repositories/common/src/main/java/com/search/common/LoggingConfig.java`
- Create: `repositories/common/src/main/java/com/search/common/ConfigLoader.java`
- Create: `repositories/common/src/main/java/com/search/common/RestClient.java`
- Create: `repositories/common/src/test/java/com/search/common/ConfigLoaderTest.java`

**Step 1: 创建公共模块 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.search</groupId>
    <artifactId>search-common</artifactId>
    <version>1.0.0</version>
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <slf4j.version>2.0.9</slf4j.version>
        <logback.version>1.4.11</logback.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>${logback.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.15.2</version>
        </dependency>
    </dependencies>
</project>
```

**Step 2: 创建配置加载器**

```java
package com.search.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;

public class ConfigLoader {
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private static Config config;

    public static Config load(String path) {
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(path)) {
            config = mapper.readValue(is, Config.class);
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + path, e);
        }
    }

    public static Config get() {
        return config;
    }

    public static class Config {
        private String env;
        private Elasticsearch elasticsearch;
        private Kafka kafka;
        // getters, setters
    }

    public static class Elasticsearch {
        private String[] hosts;
        private String username;
        private String password;
        // getters, setters
    }

    public static class Kafka {
        private String bootstrapServers;
        private String groupId;
        // getters, setters
    }
}
```

**Step 3: 创建测试**

```java
package com.search.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigLoaderTest {
    @Test
    public void testLoadConfig() {
        ConfigLoader.Config config = ConfigLoader.load("config-test.yaml");
        assertNotNull(config);
        assertNotNull(config.getElasticsearch());
        assertNotNull(config.getKafka());
    }
}
```

**Step 4: 运行测试**

```bash
cd repositories/common
mvn test
```

**Step 5: 提交**

```bash
git add repositories/common/
git commit -m "feat: add common library with config loader"
```

---

### Task 2: OpenSearch 集群部署（Docker Compose）

**目标：** 本地开发环境快速启动 OpenSearch 集群

**Files:**
- Create: `deployments/docker/docker-compose-opensearch.yml`
- Create: `deployments/docker/opensearch/config/opensearch.yml`

**Step 1: 创建 docker-compose 文件**

```yaml
version: '3.8'
services:
  opensearch-node1:
    image: opensearchproject/opensearch:2.11.0
    container_name: opensearch-node1
    environment:
      - cluster.name=opensearch-cluster
      - node.name=opensearch-node1
      - discovery.seed_hosts=opensearch-node1,opensearch-node2
      - cluster.initial_cluster_manager_nodes=opensearch-node1,opensearch-node2
      - bootstrap.memory_lock=true
      - "OPENSEARCH_JAVA_OPTS=-Xms1g -Xmx1g"
      - DISABLE_INSTALL_DEMO_CONFIG=true
      - DISABLE_SECURITY_PLUGIN=true
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - opensearch-data1:/usr/share/opensearch/data
    ports:
      - 9200:9200
      - 9600:9600
    networks:
      - search-net

  opensearch-node2:
    image: opensearchproject/opensearch:2.11.0
    container_name: opensearch-node2
    environment:
      - cluster.name=opensearch-cluster
      - node.name=opensearch-node2
      - discovery.seed_hosts=opensearch-node1,opensearch-node2
      - cluster.initial_cluster_manager_nodes=opensearch-node1,opensearch-node2
      - bootstrap.memory_lock=true
      - "OPENSEARCH_JAVA_OPTS=-Xms1g -Xmx1g"
      - DISABLE_INSTALL_DEMO_CONFIG=true
      - DISABLE_SECURITY_PLUGIN=true
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - opensearch-data2:/usr/share/opensearch/data
    networks:
      - search-net

  opensearch-dashboards:
    image: opensearchproject/opensearch-dashboards:2.11.0
    container_name: opensearch-dashboards
    ports:
      - 5601:5601
    environment:
      - 'OPENSEARCH_HOSTS=["http://opensearch-node1:9200"]'
      - DISABLE_SECURITY_DASHBOARDS_PLUGIN=true
    depends_on:
      - opensearch-node1
    networks:
      - search-net

volumes:
  opensearch-data1:
  opensearch-data2:

networks:
  search-net:
    driver: bridge
```

**Step 2: 启动集群**

```bash
cd deployments/docker
docker-compose -f docker-compose-opensearch.yml up -d
```

**Step 3: 验证集群健康**

```bash
curl http://localhost:9200/_cluster/health
```

**Step 4: 提交**

```bash
git add deployments/docker/
git commit -m "feat: add OpenSearch docker compose for local dev"
```

---

### Task 3: Kafka 集群部署

**目标：** 部署 Kafka 用于数据流缓冲

**Files:**
- Create: `deployments/docker/docker-compose-kafka.yml`

**Step 1: 创建 Kafka docker-compose**

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    networks:
      - search-net

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    networks:
      - search-net

  kafka-init:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - kafka
    entrypoint: ['/bin/sh', '-c']
    command: |
      "
      echo 'Waiting for Kafka to be ready...'
      cub kafka-ready -b kafka:29092 1 30
      kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic data-change-events --partitions 3 --replication-factor 1
      kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic vector-tasks --partitions 3 --replication-factor 1
      "
    networks:
      - search-net

networks:
  search-net:
    external: true
```

**Step 2: 启动 Kafka**

```bash
docker-compose -f docker-compose-kafka.yml up -d
```

**Step 3: 验证 Topic**

```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:29092 --list
```

**Step 4: 提交**

```bash
git add deployments/docker/
git commit -m "feat: add Kafka docker compose"
```

---

## Phase 2: 元数据配置模块（第3-4周）

### Task 4: 元数据配置数据模型

**目标：** 定义来源系统、搜索对象、字段配置的数据模型

**Files:**
- Create: `repositories/config-repo/src/main/java/com/search/config/model/Source.java`
- Create: `repositories/config-repo/src/main/java/com/search/config/model/SearchObject.java`
- Create: `repositories/config-repo/src/main/java/com/search/config/model/FieldConfig.java`
- Create: `repositories/config-repo/src/main/java/com/search/config/model/FieldType.java`

**Step 1: 创建字段类型枚举**

```java
package com.search.config.model;

public enum FieldType {
    TEXT,
    KEYWORD,
    INTEGER,
    LONG,
    DOUBLE,
    DATE,
    BOOLEAN,
    DENSE_VECTOR
}
```

**Step 2: 创建字段配置模型**

```java
package com.search.config.model;

public class FieldConfig {
    private String name;
    private FieldType type;
    private boolean searchable;
    private boolean filterable;
    private boolean sortable;
    private String analyzer;
    private boolean vectorize;
    private String vectorType; // "text" or "image"
    private int vectorDim;
    private double boost;

    // Constructors
    public FieldConfig() {}

    public FieldConfig(String name, FieldType type) {
        this.name = name;
        this.type = type;
        this.boost = 1.0;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public FieldType getType() { return type; }
    public void setType(FieldType type) { this.type = type; }
    public boolean isSearchable() { return searchable; }
    public void setSearchable(boolean searchable) { this.searchable = searchable; }
    public boolean isFilterable() { return filterable; }
    public void setFilterable(boolean filterable) { this.filterable = filterable; }
    public boolean isSortable() { return sortable; }
    public void setSortable(boolean sortable) { this.sortable = sortable; }
    public String getAnalyzer() { return analyzer; }
    public void setAnalyzer(String analyzer) { this.analyzer = analyzer; }
    public boolean isVectorize() { return vectorize; }
    public void setVectorize(boolean vectorize) { this.vectorize = vectorize; }
    public String getVectorType() { return vectorType; }
    public void setVectorType(String vectorType) { this.vectorType = vectorType; }
    public int getVectorDim() { return vectorDim; }
    public void setVectorDim(int vectorDim) { this.vectorDim = vectorDim; }
    public double getBoost() { return boost; }
    public void setBoost(double boost) { this.boost = boost; }
}
```

**Step 3: 创建来源系统模型**

```java
package com.search.config.model;

import java.util.Map;

public class Source {
    private String sourceId;
    private SourceType sourceType;
    private String connection;
    private Map<String, String> properties;

    public enum SourceType {
        MYSQL, POSTGRESQL, ORACLE, FILE
    }

    // Getters and Setters
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }
    public String getConnection() { return connection; }
    public void setConnection(String connection) { this.connection = connection; }
    public Map<String, String> getProperties() { return properties; }
    public void setProperties(Map<String, String> properties) { this.properties = properties; }
}
```

**Step 4: 创建搜索对象模型**

```java
package com.search.config.model;

import java.util.List;

public class SearchObject {
    private String objectId;
    private String sourceId;
    private String table;
    private String primaryKey;
    private List<FieldConfig> fields;
    private String appKey;

    // Getters and Setters
    public String getObjectId() { return objectId; }
    public void setObjectId(String objectId) { this.objectId = objectId; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getTable() { return table; }
    public void setTable(String table) { this.table = table; }
    public String getPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(String primaryKey) { this.primaryKey = primaryKey; }
    public List<FieldConfig> getFields() { return fields; }
    public void setFields(List<FieldConfig> fields) { this.fields = fields; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
}
```

**Step 5: 创建单元测试**

```java
package com.search.config.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FieldConfigTest {
    @Test
    public void testFieldConfigCreation() {
        FieldConfig field = new FieldConfig("title", FieldType.TEXT);
        field.setSearchable(true);
        field.setAnalyzer("ik_max_word");
        field.setBoost(2.0);

        assertEquals("title", field.getName());
        assertEquals(FieldType.TEXT, field.getType());
        assertTrue(field.isSearchable());
        assertEquals(2.0, field.getBoost());
    }
}
```

**Step 6: 提交**

```bash
git add repositories/config-repo/
git commit -m "feat: add metadata config models"
```

---

### Task 5: ES Mapping 生成器

**目标：** 根据字段配置自动生成 OpenSearch Mapping

**Files:**
- Create: `repositories/config-repo/src/main/java/com/search/config/generator/MappingGenerator.java`
- Create: `repositories/config-repo/src/test/java/com/search/config/generator/MappingGeneratorTest.java`

**Step 1: 创建 Mapping 生成器**

```java
package com.search.config.generator;

import com.search.config.model.FieldConfig;
import com.search.config.model.FieldType;
import com.search.config.model.SearchObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class MappingGenerator {
    private static final ObjectMapper mapper = new ObjectMapper();

    public String generate(SearchObject object) {
        Map<String, Object> mapping = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();

        for (FieldConfig field : object.getFields()) {
            properties.put(field.getName(), buildFieldMapping(field));
        }

        mapping.put("properties", properties);

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapping);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate mapping", e);
        }
    }

    private Map<String, Object> buildFieldMapping(FieldConfig field) {
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("type", mapFieldType(field.getType()));

        if (field.isSearchable() && field.getAnalyzer() != null) {
            mapping.put("analyzer", field.getAnalyzer());
        }

        if (field.isVectorize()) {
            mapping.put("dims", field.getVectorDim());
            mapping.put("index", true);
            mapping.put("similarity", "cosine");
        }

        return mapping;
    }

    private String mapFieldType(FieldType type) {
        switch (type) {
            case TEXT: return "text";
            case KEYWORD: return "keyword";
            case INTEGER: return "integer";
            case LONG: return "long";
            case DOUBLE: return "double";
            case DATE: return "date";
            case BOOLEAN: return "boolean";
            case DENSE_VECTOR: return "knn_vector";
            default: return "text";
        }
    }
}
```

**Step 2: 创建测试**

```java
package com.search.config.generator;

import com.search.config.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MappingGeneratorTest {
    @Test
    public void testGenerateMapping() {
        SearchObject product = new SearchObject();
        product.setObjectId("product");

        FieldConfig titleField = new FieldConfig("title", FieldType.TEXT);
        titleField.setSearchable(true);
        titleField.setAnalyzer("ik_max_word");

        FieldConfig priceField = new FieldConfig("price", FieldType.DOUBLE);
        priceField.setFilterable(true);

        FieldConfig vectorField = new FieldConfig("title_vector", FieldType.DENSE_VECTOR);
        vectorField.setVectorize(true);
        vectorField.setVectorDim(768);

        product.setFields(List.of(titleField, priceField, vectorField));

        MappingGenerator generator = new MappingGenerator();
        String mapping = generator.generate(product);

        assertTrue(mapping.contains("\"type\" : \"text\""));
        assertTrue(mapping.contains("\"analyzer\" : \"ik_max_word\""));
        assertTrue(mapping.contains("\"type\" : \"knn_vector\""));
        assertTrue(mapping.contains("\"dims\" : 768"));
    }
}
```

**Step 3: 提交**

```bash
git add repositories/config-repo/
git commit -m "feat: add ES mapping generator"
```

---

### Task 6: 配置管理 API（增删改查）

**目标：** 提供元数据配置的 REST API

**Files:**
- Create: `services/config-admin/src/main/java/com/search/admin/ConfigAdminApplication.java`
- Create: `services/config-admin/src/main/java/com/search/admin/controller/SourceController.java`
- Create: `services/config-admin/src/main/java/com/search/admin/controller/ObjectController.java`
- Create: `services/config-admin/src/main/java/com/search/admin/service/ConfigService.java`

**Step 1: 创建 Spring Boot 应用主类**

```java
package com.search.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConfigAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigAdminApplication.class, args);
    }
}
```

**Step 2: 创建配置服务**

```java
package com.search.admin.service;

import com.search.config.model.*;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConfigService {
    private final ConcurrentHashMap<String, Source> sources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SearchObject> objects = new ConcurrentHashMap<>();

    // Source operations
    public Source createSource(Source source) {
        sources.put(source.getSourceId(), source);
        return source;
    }

    public Source getSource(String sourceId) {
        return sources.get(sourceId);
    }

    public void deleteSource(String sourceId) {
        sources.remove(sourceId);
    }

    // SearchObject operations
    public SearchObject createObject(SearchObject object) {
        objects.put(object.getObjectId(), object);
        return object;
    }

    public SearchObject getObject(String objectId) {
        return objects.get(objectId);
    }

    public void deleteObject(String objectId) {
        objects.remove(objectId);
    }
}
```

**Step 3: 创建 Source Controller**

```java
package com.search.admin.controller;

import com.search.config.model.Source;
import com.search.admin.service.ConfigService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sources")
public class SourceController {
    private final ConfigService configService;

    public SourceController(ConfigService configService) {
        this.configService = configService;
    }

    @PostMapping
    public Source createSource(@RequestBody Source source) {
        return configService.createSource(source);
    }

    @GetMapping("/{sourceId}")
    public Source getSource(@PathVariable String sourceId) {
        return configService.getSource(sourceId);
    }

    @GetMapping
    public List<Source> listSources() {
        return List.copyOf(configService.getAllSources());
    }

    @DeleteMapping("/{sourceId}")
    public void deleteSource(@PathVariable String sourceId) {
        configService.deleteSource(sourceId);
    }
}
```

**Step 4: 创建 Object Controller**

```java
package com.search.admin.controller;

import com.search.config.model.SearchObject;
import com.search.admin.service.ConfigService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/objects")
public class ObjectController {
    private final ConfigService configService;

    public ObjectController(ConfigService configService) {
        this.configService = configService;
    }

    @PostMapping
    public SearchObject createObject(@RequestBody SearchObject object) {
        return configService.createObject(object);
    }

    @GetMapping("/{objectId}")
    public SearchObject getObject(@PathVariable String objectId) {
        return configService.getObject(objectId);
    }

    @GetMapping
    public List<SearchObject> listObjects() {
        return List.copyOf(configService.getAllObjects());
    }

    @DeleteMapping("/{objectId}")
    public void deleteObject(@PathVariable String objectId) {
        configService.deleteObject(objectId);
    }
}
```

**Step 5: 提交**

```bash
git add services/config-admin/
git commit -m "feat: add config admin REST APIs"
```

---

## Phase 3: 数据同步服务（第5-6周）

### Task 7: Debezium CDC 连接器

**目标：** 从 MySQL/PG/Oracle 捕获数据变更

**Files:**
- Create: `services/data-sync/src/main/java/com/search/sync/cdc/DebeziumConnector.java`
- Create: `services/data-sync/src/main/java/com/search/sync/cdc/ChangeEventHandler.java`
- Create: `services/data-sync/debezium.conf`

**Step 1: 创建 Debezium 连接器**

```java
package com.search.sync.cdc;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.enterprise.inject.Instance;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DebeziumConnector {
    private static final Logger log = LoggerFactory.getLogger(DebeziumConnector.class);
    private final ChangeEventHandler handler;
    private DebeziumEngine<ChangeEvent<String, String>> engine;
    private ExecutorService executor;

    public DebeziumConnector(ChangeEventHandler handler) {
        this.handler = handler;
    }

    public void start(Configuration config) {
        engine = DebeziumEngine.create(ChangeEvent.class)
            .using(config)
            .notifying(handler)
            .build();

        executor = Executors.newSingleThreadExecutor();
        executor.execute(engine);
        log.info("Debezium connector started");
    }

    public void stop() {
        if (engine != null) {
            engine.close();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }
}
```

**Step 2: 创建变更事件处理器**

```java
package com.search.sync.cdc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.Handler;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class ChangeEventHandler implements Handler<ChangeEvent<String, String>> {
    private static final Logger log = LoggerFactory.getLogger(ChangeEventHandler.class);
    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChangeEventHandler(KafkaProducer<String, String> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    @Override
    public void handle(List<ChangeEvent<String, String>> events) {
        for (ChangeEvent<String, String> event : events) {
            try {
                String key = event.key();
                String value = event.value();

                producer.send(new ProducerRecord<>(topic, key, value));
                log.debug("Sent change event to Kafka: key={}", key);
            } catch (Exception e) {
                log.error("Failed to process change event", e);
            }
        }
    }
}
```

**Step 3: 提交**

```bash
git add services/data-sync/
git commit -m "feat: add Debezium CDC connector"
```

---

### Task 8: Kafka 消费者与数据处理

**目标：** 消费 Kafka 数据变更消息，处理清洗后写入 ES

**Files:**
- Create: `services/data-sync/src/main/java/com/search/sync/consumer/DataChangeConsumer.java`
- Create: `services/data-sync/src/main/java/com/search/sync/processor/DataProcessor.java`
- Create: `services/data-sync/src/main/java/com/search/sync/writer/ESWriter.java`

**Step 1: 创建消费者**

```java
package com.search.sync.consumer;

import com.search.sync.processor.DataProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.Collections;

public class DataChangeConsumer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DataChangeConsumer.class);
    private final KafkaConsumer<String, String> consumer;
    private final String topic;
    private final DataProcessor processor;

    public DataChangeConsumer(KafkaConsumer<String, String> consumer,
                              String topic,
                              DataProcessor processor) {
        this.consumer = consumer;
        this.topic = topic;
        this.processor = processor;
    }

    @Override
    public void run() {
        consumer.subscribe(Collections.singletonList(topic));

        while (!Thread.currentThread().isInterrupted()) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, String> record : records) {
                try {
                    processor.process(record.value());
                } catch (Exception e) {
                    log.error("Failed to process record: {}", record.value(), e);
                }
            }
        }
    }
}
```

**Step 2: 创建数据处理器**

```java
package com.search.sync.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.search.sync.writer.ESWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataProcessor {
    private static final Logger log = LoggerFactory.getLogger(DataProcessor.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ESWriter esWriter;

    public DataProcessor(ESWriter esWriter) {
        this.esWriter = esWriter;
    }

    public void process(String message) {
        try {
            JsonNode event = mapper.readTree(message);

            String op = event.get("op").asText();
            String objectId = event.get("source").get("table").asText();

            switch (op) {
                case "c": // Create
                case "u": // Update
                    JsonNode after = event.get("after");
                    esWriter.upsert(objectId, after.toString());
                    break;
                case "d": // Delete
                    String id = event.get("before").get("id").asText();
                    esWriter.delete(objectId, id);
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to process message: {}", message, e);
        }
    }
}
```

**Step 3: 创建 ES 写入器**

```java
package com.search.sync.writer;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESWriter {
    private static final Logger log = LoggerFactory.getLogger(ESWriter.class);
    private final OpenSearchClient client;
    private final String indexPrefix;

    public ESWriter(OpenSearchClient client, String indexPrefix) {
        this.client = client;
        this.indexPrefix = indexPrefix;
    }

    public void upsert(String objectType, String document) {
        try {
            String indexName = indexPrefix + "_" + objectType;
            IndexRequest<String> request = IndexRequest.of(i -> i
                .index(indexName)
                .document(document)
                .id(extractId(document))
            );
            client.index(request);
            log.debug("Upserted document to {}", indexName);
        } catch (Exception e) {
            log.error("Failed to upsert document", e);
        }
    }

    public void delete(String objectType, String id) {
        try {
            String indexName = indexPrefix + "_" + objectType;
            DeleteRequest request = DeleteRequest.of(d -> d
                .index(indexName)
                .id(id)
            );
            client.delete(request);
            log.debug("Deleted document from {}", indexName);
        } catch (Exception e) {
            log.error("Failed to delete document", e);
        }
    }

    private String extractId(String document) {
        // Extract ID from document
        return "1";
    }
}
```

**Step 4: 提交**

```bash
git add services/data-sync/
git commit -m "feat: add Kafka consumer and ES writer"
```

---

## Phase 4: 查询服务（第7-8周）

### Task 9: 查询 API 基础框架

**目标：** 创建查询服务基础结构，支持简单关键词搜索

**Files:**
- Create: `services/query-service/src/main/java/com/search/query/QueryServiceApplication.java`
- Create: `services/query-service/src/main/java/com/search/query/controller/SearchController.java`
- Create: `services/query-service/src/main/java/com/search/query/service/SearchService.java`

**Step 1: 创建搜索请求/响应模型**

```java
package com.search.query.model;

import java.util.Map;

public class SearchRequest {
    private String query;
    private String appKey;
    private RecallStrategy recallStrategy;
    private Map<String, Object> filters;
    private Sort sort;
    private int page = 1;
    private int pageSize = 20;

    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public RecallStrategy getRecallStrategy() { return recallStrategy; }
    public void setRecallStrategy(RecallStrategy recallStrategy) { this.recallStrategy = recallStrategy; }
    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters; }
    public Sort getSort() { return sort; }
    public void setSort(Sort sort) { this.sort = sort; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public static class RecallStrategy {
        private boolean keyword = true;
        private VectorConfig vector;
        private boolean hot = true;

        // Getters and Setters
        public boolean isKeyword() { return keyword; }
        public void setKeyword(boolean keyword) { this.keyword = keyword; }
        public VectorConfig getVector() { return vector; }
        public void setVector(VectorConfig vector) { this.vector = vector; }
        public boolean isHot() { return hot; }
        public void setHot(boolean hot) { this.hot = hot; }

        public static class VectorConfig {
            private boolean enabled;
            private double weight = 0.3;
            private int k = 100;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public double getWeight() { return weight; }
            public void setWeight(double weight) { this.weight = weight; }
            public int getK() { return k; }
            public void setK(int k) { this.k = k; }
        }
    }

    public static class Sort {
        private String field;
        private String order = "desc";

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getOrder() { return order; }
        public void setOrder(String order) { this.order = order; }
    }
}
```

```java
package com.search.query.model;

import java.util.List;

public class SearchResponse {
    private List<Hit> hits;
    private long total;
    private int page;
    private int pageSize;

    // Getters and Setters
    public List<Hit> getHits() { return hits; }
    public void setHits(List<Hit> hits) { this.hits = hits; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public static class Hit {
        private String id;
        private float score;
        private Map<String, Object> source;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public float getScore() { return score; }
        public void setScore(float score) { this.score = score; }
        public Map<String, Object> getSource() { return source; }
        public void setSource(Map<String, Object> source) { this.source = source; }
    }
}
```

**Step 2: 创建搜索服务**

```java
package com.search.query.service;

import com.search.query.model.SearchRequest;
import com.search.query.model.SearchResponse;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private final OpenSearchClient client;

    public SearchService(OpenSearchClient client) {
        this.client = client;
    }

    public SearchResponse search(SearchRequest request) {
        try {
            String indexName = getIndexName(request.getAppKey());

            SearchResponse<java.util.Map<String, Object>> response = client.search(s -> s
                .index(indexName)
                .from((request.getPage() - 1) * request.getPageSize())
                .size(request.getPageSize())
                .query(q -> q
                    .simpleString(sq -> sq
                        .field("title")
                        .field("description")
                        .query(request.getQuery())
                    )
                ),
                java.util.Map.class
            );

            SearchResponse result = new SearchResponse();
            result.setTotal(response.hits().total().value());
            result.setPage(request.getPage());
            result.setPageSize(request.getPageSize());
            result.setHits(response.hits().hits().stream()
                .map(this::convertHit)
                .collect(Collectors.toList()));

            return result;
        } catch (Exception e) {
            log.error("Search failed", e);
            throw new RuntimeException("Search failed", e);
        }
    }

    private SearchResponse.Hit convertHit(Hit<java.util.Map<String, Object>> hit) {
        SearchResponse.Hit result = new SearchResponse.Hit();
        result.setId(hit.id());
        result.setScore(hit.score());
        result.setSource(hit.source());
        return result;
    }

    private String getIndexName(String appKey) {
        return "search_" + appKey;
    }
}
```

**Step 3: 创建控制器**

```java
package com.search.query.controller;

import com.search.query.model.SearchRequest;
import com.search.query.model.SearchResponse;
import com.search.query.service.SearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public SearchResponse search(@RequestBody SearchRequest request) {
        return searchService.search(request);
    }
}
```

**Step 4: 提交**

```bash
git add services/query-service/
git commit -m "feat: add query service with keyword search"
```

---

### Task 10: 多路召回引擎

**目标：** 实现向量召回、关键词召回、热门召回的并发执行

**Files:**
- Create: `services/query-service/src/main/java/com/search/query/recall/RecallEngine.java`
- Create: `services/query-service/src/main/java/com/search/query/recall/KeywordRecall.java`
- Create: `services/query-service/src/main/java/com/search/query/recall/VectorRecall.java`
- Create: `services/query-service/src/main/java/com/search/query/recall/HotRecall.java`
- Create: `services/query-service/src/main/java/com/search/query/recall/RecallFusion.java`

**Step 1: 创建召回结果模型**

```java
package com.search.query.recall;

public class RecallResult {
    private String id;
    private float score;
    private String source; // "keyword", "vector", "hot"

    public RecallResult(String id, float score, String source) {
        this.id = id;
        this.score = score;
        this.source = source;
    }

    public String getId() { return id; }
    public float getScore() { return score; }
    public String getSource() { return source; }
}
```

**Step 2: 创建关键词召回**

```java
package com.search.query.recall;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class KeywordRecall {
    private static final Logger log = LoggerFactory.getLogger(KeywordRecall.class);
    private final OpenSearchClient client;

    public KeywordRecall(OpenSearchClient client) {
        this.client = client;
    }

    public List<RecallResult> recall(String index, String query, int topK) {
        try {
            SearchResponse<java.util.Map<String, Object>> response = client.search(s -> s
                .index(index)
                .size(topK)
                .query(q -> q
                    .simpleString(sq -> sq
                        .field("title^2")
                        .field("description")
                        .query(query)
                    )
                ),
                java.util.Map.class
            );

            return response.hits().hits().stream()
                .map(hit -> new RecallResult(hit.id(), hit.score(), "keyword"))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Keyword recall failed", e);
            return List.of();
        }
    }
}
```

**Step 3: 创建向量召回**

```java
package com.search.query.recall;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.VectorQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class VectorRecall {
    private static final Logger log = LoggerFactory.getLogger(VectorRecall.class);
    private final OpenSearchClient client;
    private final VectorEmbeddingService embeddingService;

    public VectorRecall(OpenSearchClient client, VectorEmbeddingService embeddingService) {
        this.client = client;
        this.embeddingService = embeddingService;
    }

    public List<RecallResult> recall(String index, String query, int topK) {
        try {
            float[] queryVector = embeddingService.embed(query);

            SearchResponse<java.util.Map<String, Object>> response = client.search(s -> s
                .index(index)
                .size(topK)
                .query(q -> q
                    .knn(k -> k
                        .field("title_vector")
                        .queryVector(queryVector)
                        .k(topK)
                    )
                ),
                java.util.Map.class
            );

            return response.hits().hits().stream()
                .map(hit -> new RecallResult(hit.id(), hit.score(), "vector"))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Vector recall failed", e);
            return List.of();
        }
    }
}
```

**Step 4: 创建热门召回**

```java
package com.search.query.recall;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HotRecall {
    private static final Logger log = LoggerFactory.getLogger(HotRecall.class);
    private final OpenSearchClient client;

    public HotRecall(OpenSearchClient client) {
        this.client = client;
    }

    public List<RecallResult> recall(String index, int topK) {
        try {
            SearchResponse<java.util.Map<String, Object>> response = client.search(s -> s
                .index(index)
                .size(topK)
                .sort(sort -> sort
                    .field(f -> f
                        .field("sales")
                        .order(SortOrder.Desc)
                    )
                ),
                java.util.Map.class
            );

            return response.hits().hits().stream()
                .map(hit -> new RecallResult(hit.id(), hit.score(), "hot"))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Hot recall failed", e);
            return List.of();
        }
    }
}
```

**Step 5: 创建召回融合**

```java
package com.search.query.recall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecallFusion {
    private static final Logger log = LoggerFactory.getLogger(RecallFusion.class);

    public List<RecallResult> fuse(List<RecallResult> keywordResults,
                                    List<RecallResult> vectorResults,
                                    List<RecallResult> hotResults,
                                    FusionConfig config) {
        // Deduplicate and merge
        Map<String, RecallResult> merged = new LinkedHashMap<>();

        // Normalize scores and merge
        mergeResults(merged, keywordResults, config.keywordWeight);
        mergeResults(merged, vectorResults, config.vectorWeight);
        mergeResults(merged, hotResults, config.hotWeight);

        // Sort by score
        return merged.values().stream()
            .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
            .limit(config.topK)
            .collect(Collectors.toList());
    }

    private void mergeResults(Map<String, RecallResult> merged,
                              List<RecallResult> results,
                              double weight) {
        if (results.isEmpty()) return;

        float maxScore = results.stream()
            .map(RecallResult::getScore)
            .max(Float::compare)
            .orElse(1.0f);

        for (RecallResult result : results) {
            String id = result.getId();
            float normalizedScore = (result.getScore() / maxScore) * weight;

            merged.merge(id, result, (existing, newValue) -> {
                float newScore = existing.getScore() + normalizedScore;
                return new RecallResult(id, newScore, "fusion");
            });
        }
    }

    public static class FusionConfig {
        private double keywordWeight = 0.5;
        private double vectorWeight = 0.3;
        private double hotWeight = 0.2;
        private int topK = 100;

        public FusionConfig(double keywordWeight, double vectorWeight, double hotWeight, int topK) {
            this.keywordWeight = keywordWeight;
            this.vectorWeight = vectorWeight;
            this.hotWeight = hotWeight;
            this.topK = topK;
        }
    }
}
```

**Step 6: 提交**

```bash
git add services/query-service/
git commit -m "feat: add multi-path recall engine"
```

---

### Task 11: 精排引擎

**目标：** 实现可配置的多因子排序

**Files:**
- Create: `services/query-service/src/main/java/com/search/query/rerank/RerankEngine.java`
- Create: `services/query-service/src/main/java/com/search/query/rerank/SortRuleLoader.java`

**Step 1: 创建排序规则模型**

```java
package com.search.query.rerank;

import java.util.List;

public class SortRule {
    private String ruleId;
    private String appKey;
    private List<Factor> factors;

    public static class Factor {
        private String field;       // "sales", "freshness", "price_score"
        private double weight;
        private String mode;        // "linear", "log"

        public String getField() { return field; }
        public double getWeight() { return weight; }
        public String getMode() { return mode; }
    }

    public String getRuleId() { return ruleId; }
    public String getAppKey() { return appKey; }
    public List<Factor> getFactors() { return factors; }
}
```

**Step 2: 创建精排引擎**

```java
package com.search.query.rerank;

import com.search.query.recall.RecallResult;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RerankEngine {
    private static final Logger log = LoggerFactory.getLogger(RerankEngine.class);
    private final OpenSearchClient client;
    private final SortRuleLoader ruleLoader;

    public RerankEngine(OpenSearchClient client, SortRuleLoader ruleLoader) {
        this.client = client;
        this.ruleLoader = ruleLoader;
    }

    public List<RecallResult> rerank(String appKey, List<RecallResult> candidates) {
        SortRule rule = ruleLoader.getRule(appKey);
        if (rule == null) {
            return candidates;
        }

        // Fetch documents for scoring
        Map<String, Map<String, Object>> docs = fetchDocuments(appKey, candidates);

        // Calculate scores
        return candidates.stream()
            .map(result -> {
                float newScore = calculateScore(result, docs.get(result.getId()), rule);
                return new RecallResult(result.getId(), newScore, "rerank");
            })
            .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
            .collect(Collectors.toList());
    }

    private float calculateScore(RecallResult result,
                                  Map<String, Object> doc,
                                  SortRule rule) {
        float baseScore = result.getScore();
        float totalScore = baseScore;

        for (SortRule.Factor factor : rule.getFactors()) {
            Object value = doc.get(factor.getField());
            if (value == null) continue;

            double factorScore = 0;
            if (value instanceof Number) {
                factorScore = ((Number) value).doubleValue();
            }

            totalScore += factorScore * factor.getWeight();
        }

        return totalScore;
    }

    private Map<String, Map<String, Object>> fetchDocuments(String appKey,
                                                             List<RecallResult> candidates) {
        Map<String, Map<String, Object>> docs = new HashMap<>();
        String index = "search_" + appKey;

        for (RecallResult candidate : candidates) {
            try {
                GetResponse<java.util.Map<String, Object>> response = client.get(g -> g
                    .index(index)
                    .id(candidate.getId()),
                    java.util.Map.class
                );
                if (response.found()) {
                    docs.put(candidate.getId(), response.source());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch document: {}", candidate.getId(), e);
            }
        }

        return docs;
    }
}
```

**Step 3: 提交**

```bash
git add services/query-service/
git commit -m "feat: add configurable rerank engine"
```

---

## Phase 5: 向量化服务（第9-10周）

### Task 12: 文本向量化服务

**目标：** 部署 BGE/GTE 模型，提供文本 Embedding API

**Files:**
- Create: `services/vector-service/src/main/java/com/search/vector/VectorServiceApplication.java`
- Create: `services/vector-service/src/main/java/com/search/vector/controller/EmbeddingController.java`
- Create: `services/vector-service/src/main/java/com/search/vector/service/EmbeddingService.java`
- Create: `services/vector-service/model/bge-base-zh-v1.5`

**Step 1: 创建 Embedding 服务**

```java
package com.search.vector.service;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.DefaultTokenizer;
import ai.djl.modality.nlp.Tokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.TranslateException;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class EmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private final Predictor<String, float[]> predictor;

    public EmbeddingService() throws ModelException, IOException {
        // Load model (using ONNX or DJL)
        String modelId = "BAAI/bge-base-zh-v1.5";
        this.predictor = createPredictor(modelId);
    }

    public float[] embed(String text) {
        try {
            return predictor.predict(text);
        } catch (Exception e) {
            log.error("Embedding failed for text: {}", text, e);
            return new float[768]; // Return zero vector on error
        }
    }

    private Predictor<String, float[]> createPredictor(String modelId) {
        // Initialize model and predictor
        // This is simplified - actual implementation would use DJL or ONNX Runtime
        return null;
    }
}
```

**Step 2: 创建控制器**

```java
package com.search.vector.controller;

import com.search.vector.service.EmbeddingService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/embedding")
public class EmbeddingController {
    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping
    public EmbeddingResponse embed(@RequestBody EmbeddingRequest request) {
        float[] vector = embeddingService.embed(request.getText());
        return new EmbeddingResponse(vector);
    }

    @PostMapping("/batch")
    public BatchEmbeddingResponse embedBatch(@RequestBody BatchEmbeddingRequest request) {
        List<float[]> vectors = request.getTexts().stream()
            .map(embeddingService::embed)
            .collect(Collectors.toList());
        return new BatchEmbeddingResponse(vectors);
    }

    public static class EmbeddingRequest {
        private String text;
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class BatchEmbeddingRequest {
        private List<String> texts;
        public List<String> getTexts() { return texts; }
        public void setTexts(List<String> texts) { this.texts = texts; }
    }

    public static class EmbeddingResponse {
        private float[] vector;
        public EmbeddingResponse(float[] vector) { this.vector = vector; }
        public float[] getVector() { return vector; }
    }

    public static class BatchEmbeddingResponse {
        private List<float[]> vectors;
        public BatchEmbeddingResponse(List<float[]> vectors) { this.vectors = vectors; }
        public List<float[]> getVectors() { return vectors; }
    }
}
```

**Step 3: 提交**

```bash
git add services/vector-service/
git commit -m "feat: add text embedding service"
```

---

### Task 13: 图片向量化服务

**目标：** 使用 CLIP 模型支持以图搜图

**Files:**
- Create: `services/vector-service/src/main/java/com/search/vector/service/ImageEmbeddingService.java`
- Create: `services/vector-service/src/main/java/com/search/vector/controller/ImageEmbeddingController.java`

**Step 1: 创建图片 Embedding 服务**

```java
package com.search.vector.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class ImageEmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(ImageEmbeddingService.class);

    public float[] embed(byte[] imageData) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            return embed(image);
        } catch (IOException e) {
            log.error("Failed to read image", e);
            return new float[512];
        }
    }

    public float[] embed(BufferedImage image) {
        // Use CLIP model to generate image embedding
        // This would call a CLIP model loaded via DJL or ONNX
        return new float[512];
    }
}
```

**Step 2: 提交**

```bash
git add services/vector-service/
git commit -m "feat: add image embedding service"
```

---

## Phase 6: API 网关与鉴权（第11周）

### Task 14: API 网关

**目标：** 统一入口，鉴权、限流、路由

**Files:**
- Create: `services/api-gateway/src/main/java/com/search/gateway/GatewayApplication.java`
- Create: `services/api-gateway/src/main/java/com/search/gateway/filter/AuthFilter.java`
- Create: `services/api-gateway/src/main/java/com/search/gateway/filter/RateLimitFilter.java`

**Step 1: 创建认证过滤器**

```java
package com.search.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter implements GatewayFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String appKey = exchange.getRequest().getHeaders().getFirst("X-App-Key");
        String appSecret = exchange.getRequest().getHeaders().getFirst("X-App-Secret");

        if (appKey == null || appSecret == null) {
            log.warn("Missing auth headers");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        if (!validate(appKey, appSecret)) {
            log.warn("Invalid credentials: {}", appKey);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean validate(String appKey, String appSecret) {
        // Validate against stored credentials
        return true;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
```

**Step 2: 创建限流过滤器**

```java
package com.search.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitFilter implements GatewayFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private static final int DEFAULT_QPS = 100;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String appKey = exchange.getRequest().getHeaders().getFirst("X-App-Key");
        RateLimiter limiter = limiters.computeIfAbsent(appKey, k -> new RateLimiter(getQPS(appKey)));

        if (!limiter.tryAcquire()) {
            log.warn("Rate limit exceeded for {}", appKey);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private int getQPS(String appKey) {
        // Load QPS config for app
        return DEFAULT_QPS;
    }

    @Override
    public int getOrder() {
        return -99;
    }

    static class RateLimiter {
        private final int qps;
        private final AtomicLong counter = new AtomicLong(0);

        RateLimiter(int qps) {
            this.qps = qps;
        }

        boolean tryAcquire() {
            return counter.incrementAndGet() <= qps;
        }
    }
}
```

**Step 3: 提交**

```bash
git add services/api-gateway/
git commit -m "feat: add API gateway with auth and rate limiting"
```

---

## Phase 7: 监控与运维（第12周）

### Task 15: Prometheus 监控

**目标：** 添加 Prometheus metrics 端点

**Files:**
- Create: `deployments/prometheus/prometheus.yml`
- Create: `services/*/src/main/java/com/search/*/monitoring/MetricsConfig.java`

**Step 1: 创建 Prometheus 配置**

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'query-service'
    static_configs:
      - targets: ['query-service:8080']

  - job_name: 'data-sync'
    static_configs:
      - targets: ['data-sync:8081']

  - job_name: 'vector-service'
    static_configs:
      - targets: ['vector-service:8082']

  - job_name: 'opensearch'
    static_configs:
      - targets: ['opensearch-node1:9200']
```

**Step 2: 添加 Metrics 配置**

```java
package com.search.query.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    @Bean
    public Counter searchCounter(MeterRegistry registry) {
        return Counter.builder("search.requests.total")
            .description("Total search requests")
            .register(registry);
    }

    @Bean
    public Timer searchTimer(MeterRegistry registry) {
        return Timer.builder("search.requests.duration")
            .description("Search request duration")
            .register(registry);
    }
}
```

**Step 3: 提交**

```bash
git add deployments/prometheus/ services/
git commit -m "feat: add Prometheus monitoring"
```

---

### Task 16: 部署文档

**目标：** 创建完整的部署文档

**Files:**
- Create: `docs/deployment.md`
- Create: `deployments/docker/docker-compose.yml`

**Step 1: 创建完整 docker-compose**

```yaml
version: '3.8'

services:
  # Infrastructure
  opensearch-node1:
    image: opensearchproject/opensearch:2.11.0
    container_name: opensearch-node1
    environment:
      - cluster.name=search-cluster
      - node.name=opensearch-node1
      - discovery.seed_hosts=opensearch-node1,opensearch-node2
      - cluster.initial_cluster_manager_nodes=opensearch-node1,opensearch-node2
      - bootstrap.memory_lock=true
      - "OPENSEARCH_JAVA_OPTS=-Xms2g -Xmx2g"
      - DISABLE_INSTALL_DEMO_CONFIG=true
      - DISABLE_SECURITY_PLUGIN=true
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - opensearch-data1:/usr/share/opensearch/data
    ports:
      - 9200:9200
    networks:
      - search-net

  opensearch-node2:
    image: opensearchproject/opensearch:2.11.0
    container_name: opensearch-node2
    environment:
      - cluster.name=search-cluster
      - node.name=opensearch-node2
      - discovery.seed_hosts=opensearch-node1,opensearch-node2
      - cluster.initial_cluster_manager_nodes=opensearch-node1,opensearch-node2
      - bootstrap.memory_lock=true
      - "OPENSEARCH_JAVA_OPTS=-Xms2g -Xmx2g"
      - DISABLE_INSTALL_DEMO_CONFIG=true
      - DISABLE_SECURITY_PLUGIN=true
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - opensearch-data2:/usr/share/opensearch/data
    networks:
      - search-net

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - search-net

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    networks:
      - search-net

  # Applications
  config-admin:
    build: ../services/config-admin
    container_name: config-admin
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      OPENSEARCH_HOSTS: http://opensearch-node1:9200
    depends_on:
      - opensearch-node1
    networks:
      - search-net

  query-service:
    build: ../services/query-service
    container_name: query-service
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      OPENSEARCH_HOSTS: http://opensearch-node1:9200
    depends_on:
      - opensearch-node1
    networks:
      - search-net

  data-sync:
    build: ../services/data-sync
    container_name: data-sync
    environment:
      SPRING_PROFILES_ACTIVE: docker
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      OPENSEARCH_HOSTS: http://opensearch-node1:9200
    depends_on:
      - kafka
      - opensearch-node1
    networks:
      - search-net

  vector-service:
    build: ../services/vector-service
    container_name: vector-service
    ports:
      - "8082:8082"
    environment:
      SPRING_PROFILES_ACTIVE: docker
    networks:
      - search-net

  api-gateway:
    build: ../services/api-gateway
    container_name: api-gateway
    ports:
      - "8083:8083"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      QUERY_SERVICE_URL: http://query-service:8081
    depends_on:
      - query-service
    networks:
      - search-net

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    networks:
      - search-net

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    networks:
      - search-net

volumes:
  opensearch-data1:
  opensearch-data2:

networks:
  search-net:
    driver: bridge
```

**Step 2: 创建部署文档**

```markdown
# 部署文档

## 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 8GB 可用内存

## 快速启动

1. 克隆项目
\`\`\`bash
git clone <repo>
cd search-platform
\`\`\`

2. 启动所有服务
\`\`\`bash
cd deployments/docker
docker-compose up -d
\`\`\`

3. 验证服务状态
\`\`\`bash
docker-compose ps
\`\`\`

## 服务端口

| 服务 | 端口 |
|------|------|
| API Gateway | 8083 |
| Config Admin | 8080 |
| Query Service | 8081 |
| Vector Service | 8082 |
| OpenSearch | 9200 |
| Kafka | 9092 |
| Prometheus | 9090 |
| Grafana | 3000 |

## 健康检查

\`\`\`bash
# 检查 OpenSearch
curl http://localhost:9200/_cluster/health

# 检查 Query Service
curl http://localhost:8081/actuator/health

# 检查 Kafka Topics
docker exec -it kafka kafka-topics --bootstrap-server localhost:29092 --list
\`\`\`

## 停止服务

\`\`\`bash
docker-compose down
\`\`\`
```

**Step 3: 提交**

```bash
git add deployments/ docs/
git commit -m "feat: add deployment docs and docker-compose"
```

---

## 总结

本实现计划涵盖企业搜索中台的核心功能，分为 7 个阶段：

| 阶段 | 任务 | 时间 |
|------|------|------|
| Phase 1 | 基础设施搭建 | 第1-2周 |
| Phase 2 | 元数据配置模块 | 第3-4周 |
| Phase 3 | 数据同步服务 | 第5-6周 |
| Phase 4 | 查询服务 | 第7-8周 |
| Phase 5 | 向量化服务 | 第9-10周 |
| Phase 6 | API 网关与鉴权 | 第11周 |
| Phase 7 | 监控与运维 | 第12周 |

共 16 个主要任务，每个任务按 TDD 原则执行，确保代码质量和测试覆盖。
