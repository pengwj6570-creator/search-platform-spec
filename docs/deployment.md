# 部署文档

> 企业搜索中台部署指南

## 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 8GB 可用内存
- 至少 10GB 可用磁盘空间

## 快速启动

### 1. 克隆项目

```bash
git clone <repository-url>
cd search-platform-spec
```

### 2. 启动所有服务

```bash
cd deployments/docker
docker-compose up -d
```

### 3. 验证服务状态

```bash
# 检查所有服务状态
docker-compose ps

# 检查 OpenSearch 集群健康
curl http://localhost:9200/_cluster/health

# 检查 API 网关健康
curl http://localhost:8084/health
```

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| API Gateway | 8084 | 统一入口，鉴权、限流 |
| Config Admin | 8080 | 元数据配置管理 |
| Query Service | 8082 | 搜索查询服务 |
| Vector Service | 8083 | 向量化服务 |
| Data Sync | 8081 | 数据同步服务 |
| OpenSearch | 9200 | 搜索引擎 |
| OpenSearch Dashboards | 5601 | 管理界面 |
| Kafka | 9092 | 消息队列 |
| Prometheus | 9090 | 监控指标 |
| Grafana | 3000 | 监控可视化 |

## 服务说明

### API Gateway

统一入口，负责：
- 请求路由
- 身份验证
- 请求限流

### Config Admin

元数据配置管理：
- 数据源配置 (Source)
- 搜索对象配置 (SearchObject)
- 字段配置 (FieldConfig)

### Query Service

搜索查询服务：
- 关键词搜索
- 向量搜索
- 多路召回融合
- 精排

### Data Sync

数据同步服务：
- Debezium CDC
- Kafka 消费
- OpenSearch 写入

### Vector Service

向量化服务：
- 文本 Embedding
- 图片 Embedding
- BGE/GTE/CLIP 模型支持

## API 使用示例

### 1. 配置数据源

```bash
curl -X POST http://localhost:8084/api/v1/sources \
  -H "X-App-Key: app1" \
  -H "X-App-Secret: secret1" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceId": "mysql-source",
    "sourceType": "MYSQL",
    "connection": "jdbc:mysql://localhost:3306/ecommerce",
    "properties": {
      "database.hostname": "mysql",
      "database.port": "3306",
      "database.user": "root",
      "database.password": "password"
    }
  }'
```

### 2. 配置搜索对象

```bash
curl -X POST http://localhost:8084/api/v1/objects \
  -H "X-App-Key: app1" \
  -H "X-App-Secret: secret1" \
  -H "Content-Type: application/json" \
  -d '{
    "objectId": "product",
    "sourceId": "mysql-source",
    "table": "products",
    "primaryKey": "id",
    "appKey": "ecommerce",
    "fields": [
      {
        "name": "title",
        "type": "TEXT",
        "searchable": true,
        "analyzer": "ik_max_word",
        "boost": 2.0
      },
      {
        "name": "price",
        "type": "DOUBLE",
        "filterable": true
      }
    ]
  }'
```

### 3. 搜索

```bash
curl -X POST http://localhost:8084/api/v1/search \
  -H "X-App-Key: app1" \
  -H "X-App-Secret: secret1" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "手机",
    "appKey": "ecommerce",
    "page": 1,
    "pageSize": 20
  }'
```

### 4. 生成文本向量

```bash
curl -X POST http://localhost:8084/api/v1/embedding \
  -H "X-App-Key: app1" \
  -H "X-App-Secret: secret1" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "这是一段测试文本"
  }'
```

## 健康检查

```bash
# OpenSearch 集群健康
curl http://localhost:9200/_cluster/health?pretty

# 查看索引列表
curl http://localhost:9200/_cat/indices?v

# Kafka Topics
docker exec -it kafka kafka-topics --bootstrap-server localhost:29092 --list

# API Gateway
curl http://localhost:8084/health

# Config Admin
curl http://localhost:8080/actuator/health

# Query Service
curl http://localhost:8082/actuator/health
```

## 停止服务

```bash
docker-compose down
```

## 清理数据

```bash
# 停止并删除所有数据卷
docker-compose down -v

# 删除所有数据
docker-compose down -v
rm -rf data/
```

## 监控

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- OpenSearch Dashboards: http://localhost:5601

## 故障排查

### OpenSearch 无法启动

检查内存设置：
```bash
docker-compose logs opensearch-node1
```

确保系统有足够的内存：

### Kafka 连接失败

检查网络：
```bash
docker network ls
docker network inspect search-platform-spec_search-net
```

### 服务无法连接

检查服务日志：
```bash
docker-compose logs <service-name>
```

## 生产环境部署建议

1. 使用 Kubernetes 进行编排
2. 配置持久化存储
3. 启用 OpenSearch 安全插件
4. 使用外部 Kafka 集群
5. 配置日志收集 (ELK)
6. 配置告警规则
7. 定期备份数据
