# 企业搜索中台 - 完整测试运行计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 建立完整的本地单元测试 + 远程联调测试体系，验证系统功能完整性

**架构:** 分两层测试：本地单元测试 → 远程自动化集成测试（使用现有脚本）

**技术栈:** JUnit 5, Mockito, Maven, SSH, Docker Compose, bash 测试脚本

---

## 测试环境信息

| 项目 | 值 |
|------|-----|
| 远程服务器 | `ubuntu@129.226.60.225` |
| SSH密钥 | `deployments/pwj.pem` |
| 项目根目录 | `D:/dev/claudecode/search-platform-spec` |

### 已有自动化脚本

| 脚本 | 路径 | 用途 |
|------|------|------|
| 完整部署脚本 | `deployments/full-deploy.sh` | SSH配置 + 环境检查 + 部署 |
| 集成测试脚本 | `deployments/run-integration-tests.sh` | 6组自动化集成测试 |
| 环境检查脚本 | `deployments/check-remote.sh` | 服务器环境检查 |
| 服务状态脚本 | `deployments/check-services.sh` | 远程服务状态检查 |

---

## 第一阶段：本地单元测试

> 本阶段在各模块本地运行单元测试，验证代码逻辑正确性

### Task 1: 运行 data-sync 模块单元测试

**Files:**
- Test: `services/data-sync/src/test/java/com/search/sync/vectorization/VectorizationQueueTest.java` (19个测试)
- Test: `services/data-sync/src/test/java/com/search/sync/vectorization/VectorizationServiceTest.java` (17个测试)
- Test: `services/data-sync/src/test/java/com/search/sync/vectorization/VectorizationTaskTest.java` (14个测试)
- Test: `services/data-sync/src/test/java/com/search/sync/processor/DataProcessorTest.java` (21个测试)

**Step 1: 进入 data-sync 目录**

```bash
cd services/data-sync
```

**Step 2: 运行所有单元测试**

运行: `mvn test`
预期输出:
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.search.sync.vectorization.VectorizationQueueTest
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.search.sync.vectorization.VectorizationServiceTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.search.sync.vectorization.VectorizationTaskTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.search.sync.processor.DataProcessorTest
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

**Step 3: 如有测试失败，记录错误**

运行: `mvn test > ../../test-results-data-sync.log 2>&1`

**Step 4: 返回根目录**

```bash
cd ../..
```

**Step 5: 提交测试结果**

```bash
git add services/data-sync/
git commit -m "test: data-sync unit tests passed (71 tests)"
```

---

### Task 2: 运行 query-service 模块单元测试

**Files:**
- Modify: `services/query-service/src/main/java/com/search/query/recall/KeywordRecall.java`
- Modify: `services/query-service/src/main/java/com/search/query/recall/VectorRecall.java`
- Modify: `services/query-service/src/main/java/com/search/query/recall/HotRecall.java`
- Test: `services/query-service/src/test/java/com/search/query/recall/RecallEngineTest.java` (21个测试)
- Test: `services/query-service/src/test/java/com/search/query/recall/RecallResultTest.java` (13个测试)

**Step 1: 检查 query-service 编译状态**

```bash
cd services/query-service
mvn clean compile
```

预期: 可能编译失败 (OpenSearch 客户端 API 兼容性问题)

**Step 2: 如编译失败，分析并修复错误**

运行: `mvn clean compile 2>&1 | grep -A 5 "ERROR"`

常见修复方案:
- 检查 `pom.xml` OpenSearch 版本 (应为 2.6.0)
- 更新 API 调用以匹配版本
- 修复 `FieldValue` 类导入路径
- 修复 `KnnQuery.builder()` 方法签名

**Step 3: 验证修复后编译成功**

运行: `mvn clean package -DskipTests`
预期: BUILD SUCCESS

**Step 4: 运行单元测试**

运行: `mvn test`
预期:
```
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
```

**Step 5: 返回根目录**

```bash
cd ../..
```

**Step 6: 提交修复和测试结果**

```bash
git add services/query-service/
git commit -m "fix: resolve query-service compilation and pass unit tests (34 tests)"
```

---

### Task 3: 运行 config-repo 模块单元测试

**Files:**
- Test: `repositories/config-repo/src/test/java/com/search/config/model/FieldConfigTest.java` (8个测试)
- Test: `repositories/config-repo/src/test/java/com/search/config/generator/MappingGeneratorTest.java`

**Step 1: 运行 config-repo 测试**

```bash
cd repositories/config-repo
mvn test
```

**Step 2: 返回根目录**

```bash
cd ../..
```

**Step 3: 提交结果**

```bash
git add repositories/config-repo/
git commit -m "test: config-repo unit tests passed"
```

---

### Task 4: 运行 common 模块单元测试

**Files:**
- Test: `repositories/common/src/test/java/com/search/common/ConfigLoaderTest.java`

**Step 1: 运行 common 测试**

```bash
cd repositories/common
mvn test
```

**Step 2: 返回根目录**

```bash
cd ../..
```

**Step 3: 提交结果**

```bash
git add repositories/common/
git commit -m "test: common module unit tests passed"
```

---

### Task 5: 生成单元测试报告摘要

**Step 1: 创建测试摘要**

```bash
cat > TEST_SUMMARY_$(date +%Y%m%d).md << 'EOF'
# 单元测试摘要

## 测试日期
$(date '+%Y-%m-%d %H:%M:%S')

## 模块测试结果

| 模块 | 测试数 | 通过 | 失败 | 跳过 |
|------|--------|------|------|------|
| data-sync | 71 | | | |
| query-service | 34 | | | |
| config-repo | 8+ | | | |
| common | ? | | | |
| **总计** | **113+** | | | |

## 问题记录

(记录测试过程中发现的问题)
EOF
cat TEST_SUMMARY_*.md
```

**Step 2: 提交测试摘要**

```bash
git add TEST_SUMMARY_*.md
git commit -m "test: add unit test summary"
```

---

## 第二阶段：远程自动化集成测试

> 使用 `deployments/run-integration-tests.sh` 脚本执行6组自动化集成测试

### Task 6: 环境准备 - SSH 连接验证

**Files:**
- Use: `deployments/pwj.pem`
- Use: `deployments/check-remote.sh`

**Step 1: 测试 SSH 连接**

```bash
cd deployments
ssh -i pwj.pem -o ConnectTimeout=10 -o StrictHostKeyChecking=no ubuntu@129.226.60.225 "echo 'SSH连接成功'"
```

预期输出: `SSH连接成功`

**Step 2: 如 SSH 连接失败，配置密钥**

```bash
chmod 600 pwj.pem
ssh -i pwj.pem ubuntu@129.226.60.225 "echo 'SSH连接成功'"
```

**Step 3: 运行环境检查脚本**

```bash
bash check-remote.sh ubuntu@129.226.60.225
```

预期输出包含:
```
========================================
🔍 远程服务器环境检查
========================================
主机: ubuntu@129.226.60.225
========================================

系统信息
  操作系统: Ubuntu 22.04.x LTS
  内核版本: 5.15.0-xx-generic
  架构:     x86_64

硬件资源
  CPU 核心数: 4
  内存总量:   16Gi
  可用内存:   xGi
  磁盘使用:   xxGi / 100Gi

软件版本
  Docker:    ✓ Docker version 29.2.0
  Compose v2: ✓ Docker Compose version v2.23.0
  Git:       ✓ git version 2.43.0

端口占用检查
  9200 (OpenSearch): ✓ 可用
  9092 (Kafka):        ✓ 可用
  ...
```

---

### Task 7: 运行自动化集成测试脚本

**Files:**
- Use: `deployments/run-integration-tests.sh`

**Step 1: 执行集成测试脚本**

```bash
cd /d/dev/claudecode/search-platform-spec/deployments
bash run-integration-tests.sh ubuntu@129.226.60.225
```

**Step 2: 观察测试执行过程**

脚本将依次执行6组测试:

```
========================================
🧪 企业搜索中台 - 自动化集成测试
========================================
测试目标:  ubuntu@129.226.60.225
开始时间:  YYYY-MM-DD HH:MM:SS
========================================

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
【第一组】基础设施健康检查
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  测试 1: SSH 连接 ... ✓ PASS
  测试 2: Docker 运行 ... ✓ PASS
  测试 3: OpenSearch 集群健康 ... ✓ PASS
  测试 4: Kafka 端口监听 ... ✓ PASS

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
【第二组】应用服务健康检查
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  测试 5: Config Admin 健康 ... ✓ PASS
  测试 6: Query Service 健康 ... (可能失败，未部署)
  测试 7: Vector Service 健康 ... ✓ PASS
  测试 8: API Gateway 健康 ... ✓ PASS
  测试 9: Prometheus 端点 ... ✓ PASS
  测试 10: Grafana 端点 ... ✓ PASS

... (后续测试组)
```

**Step 3: 分析测试结果**

脚本最后会输出测试结果汇总:

```
========================================
📊 测试结果汇总
========================================
测试时间: YYYY-MM-DD HH:MM:SS
总测试数: 20+
通过: X
失败: Y
```

**Step 4: 如全部通过**

系统健康，可以继续开发

**Step 5: 如有失败**

根据脚本输出的建议进行修复:
- 查看服务日志: `ssh ubuntu@129.226.60.225 'cd ~/search-platform-spec/deployments/docker && docker compose logs -f'`
- 重启失败的服务: `ssh ubuntu@129.226.60.225 'cd ~/search-platform-spec/deployments/docker && docker compose restart'`
- 检查容器状态: `ssh ubuntu@129.226.60.225 'docker ps -a'`

---

### Task 8: 手动验证关键 API 端点

**目的:** 补充自动化脚本未覆盖的 API 测试

**Step 1: 验证 OpenSearch API**

```bash
# 集群健康
curl -s http://129.226.60.225:9200/_cluster/health | jq '.status, .number_of_nodes'

# 索引列表
curl -s http://129.226.60.225:9200/_cat/indices?v
```

**Step 2: 验证 Vector Service API**

```bash
# 文本向量化
curl -X POST http://129.226.60.225:8083/api/v1/embedding/text \
  -H "Content-Type: application/json" \
  -d '{"text": "hello world"}' | jq '.'
```

预期: 返回包含 `embedding` 数组的 JSON

**Step 3: 验证 Config Admin API**

```bash
# 获取 Sources 列表
curl -s http://129.226.60.225:8080/api/v1/sources | jq '.'

# 获取 Objects 列表
curl -s http://129.226.60.225:8080/api/v1/objects | jq '.'
```

**Step 4: 验证 API Gateway 路由**

```bash
# 通过 Gateway 访问 Config Admin
curl -s http://129.226.60.225:8084/config-admin/api/v1/sources | jq '.'

# Gateway 健康检查
curl -s http://129.226.60.225:8084/actuator/health | jq '.'
```

---

### Task 9: 验证服务间通信（Kafka）

**Step 1: 检查 Kafka Topics**

```bash
ssh -i pwj.pem ubuntu@129.226.60.225 \
  "docker exec kafka kafka-topics --bootstrap-server localhost:29092 --list"
```

**Step 2: 检查 data-sync 服务状态**

```bash
ssh -i pwj.pem ubuntu@129.226.60.225 \
  "docker logs data-sync --tail 30 | grep -i 'consumer\|processing\|kafka'"
```

**Step 3: 检查向量化队列**

```bash
ssh -i pwj.pem ubuntu@129.226.60.225 \
  "docker logs data-sync --tail 30 | grep -i 'vectorization\|queue'"
```

---

### Task 10: 监控指标验证

**Step 1: 验证 Prometheus 指标端点**

```bash
# Prometheus 健康检查
curl -s http://129.226.60.225:9090/-/healthy

# 各服务的 Prometheus 指标
curl -s http://129.226.60.225:8080/actuator/prometheus | head -20
curl -s http://129.226.60.225:8083/actuator/prometheus | head -20
curl -s http://129.226.60.225:8084/actuator/prometheus | head -20
```

**Step 2: 验证 Grafana 数据源**

```bash
curl -s http://129.226.60.225:3000/api/health | jq '.'
```

预期: `{"database":"ok"}`

**Step 3: 访问 Web 界面（手动验证）**

在浏览器中打开:
- OpenSearch Dashboards: http://129.226.60.225:5601
- Grafana: http://129.226.60.225:3000 (admin/admin)
- Prometheus: http://129.226.60.225:9090

---

### Task 11: 部署并测试 query-service（如修复成功）

**前提条件:** Task 2 已完成编译修复

**Step 1: 本地构建 query-service**

```bash
cd services/query-service
mvn clean package spring-boot:repackage -DskipTests
```

**Step 2: 上传 JAR 到远程服务器**

```bash
scp -i ../deployments/pwj.pem target/query-service-1.0.0.jar \
  ubuntu@129.226.60.225:~/search-platform-spec/services/query-service/target/
```

**Step 3: 远程启动 query-service**

```bash
ssh -i ../deployments/pwj.pem ubuntu@129.226.60.225 \
  "cd ~/search-platform-spec/deployments/docker && docker compose up -d query-service"
```

**Step 4: 等待服务启动并验证**

```bash
sleep 15
curl -s http://129.226.60.225:8082/actuator/health | jq '.'
```

预期: `{"status":"UP"}`

**Step 5: 测试搜索 API**

```bash
curl -X POST http://129.226.60.225:8082/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "test",
    "pageSize": 10,
    "recallStrategy": {
      "keyword": true,
      "vector": false,
      "hot": false
    }
  }' | jq '.'
```

**Step 6: 重新运行集成测试验证**

```bash
cd /d/dev/claudecode/search-platform-spec/deployments
bash run-integration-tests.sh ubuntu@129.226.60.225
```

---

### Task 12: 生成完整测试报告

**Step 1: 创建测试报告**

```bash
cd /d/dev/claudecode/search-platform-spec

cat > TEST_REPORT_$(date +%Y%m%d_%H%M%S).md << 'EOF'
# 企业搜索中台 - 测试报告

## 测试信息

| 项目 | 值 |
|------|-----|
| 测试日期 | $(date '+%Y-%m-%d %H:%M:%S') |
| 测试人员 | AI Assistant |
| 远程服务器 | ubuntu@129.226.60.225 |
| 执行脚本 | deployments/run-integration-tests.sh |

---

## 单元测试结果

### 本地单元测试

| 模块 | 测试数 | 通过 | 失败 | 状态 |
|------|--------|------|------|------|
| data-sync | 71 | | | ⏳ |
| query-service | 34 | | | ⏳ |
| config-repo | 8+ | | | ⏳ |
| common | ? | | | ⏳ |

---

## 集成测试结果

### 第一组：基础设施健康检查

| 测试项 | 状态 | 备注 |
|--------|------|------|
| SSH 连接 | ⏳ | |
| Docker 运行 | ⏳ | |
| OpenSearch 集群健康 | ⏳ | |
| Kafka 端口监听 | ⏳ | |

### 第二组：应用服务健康检查

| 服务 | 端口 | 健康端点 | 状态 |
|------|------|----------|------|
| Config Admin | 8080 | /actuator/health | ⏳ |
| Query Service | 8082 | /actuator/health | ⏳ |
| Vector Service | 8083 | /actuator/health | ⏳ |
| API Gateway | 8084 | /actuator/health | ⏳ |
| Prometheus | 9090 | /-/healthy | ⏳ |
| Grafana | 3000 | /api/health | ⏳ |

### 第三组：API 功能测试

| API | 端点 | 状态 |
|-----|------|------|
| OpenSearch 索引列表 | GET :9200/_cat/indices | ⏳ |
| Kafka Topic 列表 | docker exec | ⏳ |
| Config Admin Sources | GET :8080/api/v1/sources | ⏳ |
| Config Admin Objects | GET :8080/api/v1/objects | ⏳ |

### 第四组：容器状态检查

| 容器 | 状态 | 备注 |
|------|------|------|
| opensearch-node1 | ⏳ | |
| kafka | ⏳ | |
| zookeeper | ⏳ | |
| config-admin | ⏳ | |
| query-service | ⏳ | (可能未部署) |
| data-sync | ⏳ | |
| vector-service | ⏳ | |
| api-gateway | ⏳ | |
| prometheus | ⏳ | |
| grafana | ⏳ | |

### 第五组：资源使用检查

| 资源 | 使用情况 | 状态 |
|------|----------|------|
| CPU | ⏳ | |
| 内存 | ⏳ | |
| 磁盘 | ⏳ | |

### 第六组：日志采样检查

| 服务 | 错误日志 | 状态 |
|------|----------|------|
| config-admin | ⏳ | |
| query-service | ⏳ | |
| vector-service | ⏳ | |
| api-gateway | ⏳ | |

---

## 手动 API 验证

### Vector Service 文本向量化

```bash
curl -X POST http://129.226.60.225:8083/api/v1/embedding/text \
  -H "Content-Type: application/json" \
  -d '{"text": "hello world"}'
```

结果: ⏳

### Config Admin Sources API

```bash
curl http://129.226.60.225:8080/api/v1/sources
```

结果: ⏳

### API Gateway 路由

```bash
curl http://129.226.60.225:8084/config-admin/api/v1/sources
```

结果: ⏳

---

## 问题记录

| ID | 问题描述 | 严重程度 | 状态 |
|----|----------|----------|------|
| 1 | query-service 未部署（编译错误） | P1 | ⏳ |
| 2 | - | - | - |

---

## 测试结论

(测试完成后填写)

EOF
```

**Step 2: 提交测试报告**

```bash
git add TEST_REPORT_*.md
git commit -m "test: add comprehensive test report"
```

---

## 测试检查清单

### 本地单元测试

- [ ] data-sync: 71个测试全部通过
- [ ] query-service: 34个测试全部通过 (需先修复编译)
- [ ] config-repo: 8+个测试全部通过
- [ ] common: 测试全部通过
- [ ] 单元测试通过率 = 100%

### 远程集成测试（自动化脚本）

- [ ] 【第一组】SSH 连接通过
- [ ] 【第一组】Docker 运行正常
- [ ] 【第一组】OpenSearch 集群健康
- [ ] 【第一组】Kafka 端口监听正常
- [ ] 【第二组】Config Admin 健康 UP
- [ ] 【第二组】Query Service 健康 UP (如已部署)
- [ ] 【第二组】Vector Service 健康 UP
- [ ] 【第二组】API Gateway 健康 UP
- [ ] 【第二组】Prometheus 可访问
- [ ] 【第二组】Grafana 可访问
- [ ] 【第三组】OpenSearch API 可访问
- [ ] 【第三组】Kafka Topics 列表正常
- [ ] 【第三组】Config Admin Sources API 可访问
- [ ] 【第三组】Config Admin Objects API 可访问
- [ ] 【第四组】所有容器运行中
- [ ] 【第五组】资源使用正常
- [ ] 【第六组】无严重错误日志

### 手动 API 验证

- [ ] Vector Service 文本向量化正常
- [ ] Config Admin Sources API 返回正常
- [ ] API Gateway 路由正常
- [ ] Prometheus 指标端点可访问
- [ ] Grafana 数据源正常

---

## 故障排查指南

### 1. 单元测试失败

**症状**: `mvn test` 返回 FAILURE

**排查步骤**:
```bash
# 检查 Java 版本
java -version  # 需要 17+

# 清理并重新构建
mvn clean compile

# 查看详细错误
mvn test -X

# 检查依赖冲突
mvn dependency:tree
```

### 2. SSH 连接失败

**症状**: `Connection refused` 或 `Connection timeout`

**排查步骤**:
```bash
# 检查网络
ping 129.226.60.225

# 检查 SSH 端口
telnet 129.226.60.225 22

# 验证密钥权限
ls -la deployments/pwj.pem  # 应为 600 或 400

# 使用详细模式
ssh -vvv -i deployments/pwj.pem ubuntu@129.226.60.225
```

### 3. 服务健康检查失败

**症状**: `/actuator/health` 返回 DOWN 或无响应

**排查步骤**:
```bash
# 检查容器状态
ssh -i deployments/pwj.pem ubuntu@129.226.60.225 'docker ps -a'

# 查看服务日志
ssh -i deployments/pwj.pem ubuntu@129.226.60.225 'docker logs <service-name> --tail 50'

# 重启服务
ssh -i deployments/pwj.pem ubuntu@129.226.60.225 'cd ~/search-platform-spec/deployments/docker && docker compose restart <service-name>'
```

### 4. OpenSearch 连接失败

**症状**: 无法连接到 9200 端口

**排查步骤**:
```bash
# 检查 OpenSearch 容器
ssh -i deployments/pwj.pem ubuntu@129.226.60.225 'docker ps | grep opensearch'

# 检查集群健康
curl http://129.226.60.225:9200/_cluster/health

# 查看日志
ssh -i deployments/pwj.pem ubuntu@129.226.60.225 'docker logs opensearch-node1 --tail 50'
```

---

## 测试脚本使用速查

### 快速执行命令

```bash
# 进入部署目录
cd D:/dev/claudecode/search-platform-spec/deployments

# 环境检查
bash check-remote.sh ubuntu@129.226.60.225

# 集成测试
bash run-integration-tests.sh ubuntu@129.226.60.225

# 完整部署（如需要）
bash full-deploy.sh ubuntu@129.226.60.225
```

### 测试结果判断标准

**健康标准**:
- ✅ 所有 6 组测试全部通过
- ✅ 容器状态全部为 `Up`
- ✅ 健康检查全部返回 `UP`
- ✅ 无错误日志

**警告标准**:
- ⚠️ 部分容器重启中
- ⚠️ 资源使用率 > 80%
- ⚠️ 有少量警告日志

**异常标准**:
- ❌ 任何容器未启动
- ❌ 健康检查失败
- ❌ 端口无法访问
- ❌ 有大量错误日志

---

## 相关文档

| 文档 | 路径 |
|------|------|
| 自动化测试指南 | `docs/testing/AUTOMATED_TESTING.md` |
| 部署快速入门 | `deployments/QUICKSTART.md` |
| 远程部署指南 | `deployments/REMOTE_DEPLOYMENT.md` |
| 部署进度记录 | `deployments/DEPLOYMENT_PROGRESS.md` |
| SSH 密钥配置 | `docs/deployment/SSH_KEY_SETUP.md` |

---

**END OF PLAN**
