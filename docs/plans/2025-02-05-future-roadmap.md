# 企业搜索中台 - 后续计划

> 创建日期：2025-02-05
> 更新日期：2026-02-06
> 状态：待执行

---

## 概述

基础版本（Phase 1-7）已完成，包括数据同步、查询服务、向量化服务、API网关、监控运维和配置管理前端。

本文档列出后续优化和增强功能的计划任务。

---

## Phase 7.5: 基线验证（新增）

> **说明：** 在进入生产就绪优化前，需要先完成基线功能验证，确保核心业务流程完整可用。

### Task 16.1: 数据同步完整流程验证

**目标：** 验证 CDC → Kafka → Data Sync → OpenSearch 完整数据流

**验证步骤：**
1. 模拟数据库变更事件
2. 发送到 Kafka `data-change-events` topic
3. 验证 data-sync 消费处理
4. 验证向量化任务执行
5. 验证 OpenSearch 数据正确索引

**Files:**
- Test: `deployments/test-data-sync-flow.sh`
- Update: `docs/testing/BUSINESS_FUNCTION_TESTS.md`

---

### Task 16.2: 向量召回端到端测试

**目标：** 验证 KNN 向量搜索完整流程

**验证步骤：**
1. 准备包含向量字段的测试数据
2. 通过 Vector Service 生成向量
3. 写入 OpenSearch knn_vector 字段
4. 执行向量搜索查询
5. 验证结果相关性和准确性

**Files:**
- Test: `deployments/test-vector-recall.sh`
- Data: `deployments/test-data/vector-test-data.json`

---

### Task 16.3: 索引自动创建实现

**目标：** Config Admin 创建 SearchObject 时自动创建 OpenSearch 索引

**当前问题：**
- 创建 SearchObject 后需要手动创建索引
- MappingGenerator 已实现但未调用

**方案：**
```java
// ConfigService.createObject() 中添加
if (object.getFields() != null) {
    String mapping = mappingGenerator.generate(object);
    openSearchClient.createIndex(object.getObjectId(), mapping);
}
```

**Files:**
- Update: `services/config-admin/src/main/java/com/search/admin/service/ConfigService.java`
- Update: `services/config-admin/src/main/java/com/search/admin/controller/ObjectController.java`
- Add: `services/config-admin/src/main/java/com/search/admin/indices/IndexService.java`

---

### Task 16.4: 多路融合召回验证

**目标：** 验证关键词+向量+热度三路召回融合

**验证步骤：**
1. 准备测试数据（包含向量字段）
2. 配置融合策略权重
3. 执行混合搜索查询
4. 验证结果融合正确性

**Files:**
- Update: `services/query-service/src/main/java/com/search/query/recall/RecallEngine.java`
- Test: `deployments/test-hybrid-recall.sh`

---

### Task 16.5: IK 中文分词器安装

**目标：** 安装 IK 分词器提升中文搜索效果

**步骤：**
```bash
# 在 OpenSearch 容器中安装
docker exec opensearch-node1 \
  plugin install https://github.com/infinilabs/analysis-ik/releases/download/v2.11.0/opensearch-analysis-ik-2.11.0.zip

# 重启容器
docker restart opensearch-node1
```

**Files:**
- Add: `deployments/opensearch/install-ik-plugin.sh`
- Update: `deployments/docker/docker-compose.yml`

---

## Phase 8: 生产就绪优化

### Task 17: 持久化向量化队列

**目标：** 将内存队列替换为 Kafka/Redis，提高可靠性和扩展能力

**当前问题：**
- VectorizationQueue 使用内存存储，服务重启后任务丢失
- 单机处理能力有限，无法水平扩展

**方案：**
```
VectorizationQueue → Kafka Topic "vector-tasks"
    ↓
多个 VectorizationConsumer 消费者组
    ↓
水平扩展处理能力
```

**Files:**
- Update: `services/data-sync/src/main/java/com/search/sync/vectorization/VectorizationQueue.java`
- Create: `services/data-sync/src/main/java/com/search/sync/vectorization/KafkaVectorizationProducer.java`
- Create: `services/data-sync/src/main/java/com/search/sync/vectorization/KafkaVectorizationConsumer.java`

---

### Task 18: 配置中心集成

**目标：** 从 Nacos/ETCD 加载字段配置，替代硬编码检测逻辑

**当前问题：**
- DataProcessor 使用硬编码逻辑检测向量字段
- 无法动态调整配置

**方案：**
```
ConfigAdminService → 写入配置到 Nacos
    ↓
DataSyncService → 订阅 Nacos 配置变更
    ↓
动态更新向量化检测规则
```

**Files:**
- Create: `repositories/config-repo/src/main/java/com/search/config/center/NacosConfigClient.java`
- Update: `services/data-sync/src/main/java/com/search/sync/processor/DataProcessor.java`
- Add: `application.yml` Nacos 配置

---

### Task 19: 向量化任务监控

**目标：** 添加向量化任务的 Prometheus 指标

**监控指标：**
- `vectorization_queue_size` - 队列长度
- `vectorization_tasks_processed_total` - 已处理任务数
- `vectorization_tasks_failed_total` - 失败任务数
- `vectorization_duration_seconds` - 处理耗时

**Files:**
- Update: `services/data-sync/src/main/java/com/search/sync/vectorization/VectorizationQueue.java`
- Update: `deployments/prometheus/prometheus.yml`

---

## Phase 9: 搜索能力增强

### Task 20: 同义词词典

**目标：** 支持同义词扩展，提升召回率

**示例：**
- 用户搜索"手机" → 也匹配"移动电话"
- 用户搜索"笔记本" → 也匹配"笔记本电脑"

**方案：**
1. 支持同义词配置文件上传
2. 使用 OpenSearch Synonym Token Filter
3. 管理界面支持同义词增删改查

**Files:**
- Create: `services/config-admin/src/main/java/com/search/admin/controller/SynonymController.java`
- Update: `repositories/config-repo/src/main/java/com/search/config/generator/MappingGenerator.java`
- Add: 前端同义词管理页面

---

### Task 21: 拼写纠错

**目标：** 自动纠正用户输入的拼写错误

**方案：**
1. 使用 OpenSearch Term Suggester
2. 配置模糊匹配阈值
3. 返回纠错建议

**Files:**
- Update: `services/query-service/src/main/java/com/search/query/service/SearchService.java`
- Update: `services/query-service/src/main/java/com/search/query/model/SearchResponse.java`

---

### Task 22: 搜索结果缓存

**目标：** 缓存热门查询结果，降低 OpenSearch 压力

**方案：**
1. 使用 Redis 缓存热门查询
2. TTL 设置 5-10 分钟
3. 缓存 Key: `search:{appKey}:{query_hash}`

**Files:**
- Create: `services/query-service/src/main/java/com/search/query/cache/SearchCacheService.java`
- Add: Redis 依赖配置

---

## Phase 10: 高级功能

### Task 23: A/B 测试框架

**目标：** 支持不同排序策略的 A/B 对比

**方案：**
1. 定义实验配置（流量分配、策略参数）
2. 请求时根据用户 ID 分流
3. 记录实验指标（CTR、转化率）

**Files:**
- Create: `services/query-service/src/main/java/com/search/query/abtest/ExperimentConfig.java`
- Create: `services/query-service/src/main/java/com/search/query/abtest/ExperimentService.java`

---

### Task 24: 搜索分析报表

**目标：** 提供搜索数据分析面板

**指标：**
- PV/UV
- 热门搜索词
- 零结果搜索
- 平均搜索延迟
- 转化漏斗

**Files:**
- Create: `services/query-service/src/main/java/com/search/query/analytics/SearchAnalytics.java`
- Add: `deployments/grafana/dashboards/search-analytics.json`

---

### Task 25: 个性化排序

**目标：** 基于用户历史行为个性化排序

**方案：**
1. 收集用户点击/购买行为
2. 生成用户向量
3. 召回时计算用户-商品相似度

**Files:**
- Create: `services/personalization/src/main/java/com/search/personal/UserVectorService.java`
- Update: `services/query-service/src/main/java/com/search/query/rerank/RerankEngine.java`

---

## Phase 11: 运维增强

### Task 26: 索引生命周期管理

**目标：** 自动管理索引的创建、rollover、删除

**方案：**
1. 配置索引保留策略（如 30 天）
2. 定时rollover大索引
3. 自动删除过期索引

**Files:**
- Create: `services/data-sync/src/main/java/com/search/sync/lifecycle/IndexLifecycleManager.java`

---

### Task 27: 数据一致性校验

**目标：** 定时校验源数据库与 OpenSearch 数据一致性

**方案：**
1. 定时抽取样本数据
2. 对比记录数和内容
3. 发现不一致时报警和补偿

**Files:**
- Create: `services/data-sync/src/main/java/com/search/sync/validator/DataConsistencyValidator.java`

---

### Task 28: 灰度发布支持

**目标：** 支持新版本索引的灰度验证

**方案：**
1. 创建新版本索引（如 v2）
2. 部分流量写入新索引
3. 验证后全量切换

**Files:**
- Update: `services/data-sync/src/main/java/com/search/sync/writer/ESWriter.java`
- Add: 灰度流量配置

---

## 优先级建议

| 优先级 | 任务 | 原因 |
|--------|------|------|
| **P0 (基线)** | Task 16.1 - 16.5 | 核心流程验证，必须先完成 |
| **P1 (高)** | Task 17, 18, 19 | 生产稳定性必需 |
| **P2 (中)** | Task 20, 22 | 核心搜索体验提升 |
| **P3 (低)** | Task 21, 23, 24 | 增值功能 |
| **P4 (未来)** | Task 25, 26, 27, 28 | 高级运维能力 |

---

## 预估工作量

| Phase | 任务数 | 预估时间 |
|-------|--------|----------|
| Phase 7.5 (基线验证) | 5 | 1 周 |
| Phase 8 | 3 | 1-2 周 |
| Phase 9 | 3 | 2-3 周 |
| Phase 10 | 3 | 2-3 周 |
| Phase 11 | 3 | 1-2 周 |

**总计：** 17 个任务，约 7-11 周完成

---

## 更新记录

| 日期 | 更新内容 |
|------|---------|
| 2025-02-05 | 初始版本，Phase 8-11 |
| 2026-02-06 | 新增 Phase 7.5 基线验证阶段，调整优先级 |
