# 企业搜索中台 - 项目上下文

> **用途**: 新会话开始时，请先阅读本文档了解项目背景
> **更新日期**: 2026-02-06

---

## 一、项目概述

### 目标
构建一个**企业级搜索中台**，提供统一的搜索服务，支持多数据源接入、向量化搜索、实时数据同步。

### 核心能力
- 多数据源配置管理（MySQL, PostgreSQL, 文件等）
- 实时数据同步（CDC → Kafka → OpenSearch）
- 多路召回融合（关键词 + 向量 + 热度）
- 统一 API 网关（认证 + 限流 + 路由）

### 技术栈
| 类别 | 技术 |
|------|------|
| 搜索引擎 | OpenSearch 2.11.0 |
| 消息队列 | Kafka + Zookeeper |
| 后端框架 | Spring Boot 3.1.5 + Java 17 |
| 向量模型 | bge-base-zh-v1.5 (768维) |
| 监控 | Prometheus + Grafana |
| 网关 | Spring Cloud Gateway |

---

## 二、项目结构

```
search-platform-spec/
├── services/                    # 微服务
│   ├── config-admin/           # 配置管理服务 (8080)
│   ├── query-service/          # 查询服务 (8082)
│   ├── vector-service/         # 向量化服务 (8083)
│   ├── data-sync/              # 数据同步服务 (8081)
│   └── api-gateway/            # API 网关 (8084)
├── repositories/               # 代码库
│   ├── config-repo/           # 配置模型
│   └── common/                # 公共组件
├── deployments/                # 部署相关
│   ├── docker/                # Docker Compose 配置
│   ├── prometheus/            # 监控配置
│   └── *.sh                   # 部署脚本
├── docs/                       # 文档
│   ├── plans/                 # 开发计划
│   └── testing/               # 测试文档
└── PROJECT_CONTEXT.md         # 本文件
```

---

## 三、当前状态

### 已完成 (Phase 1-7)

| 模块 | 状态 | 说明 |
|------|------|------|
| 配置管理 | ✅ | Source/Object CRUD API |
| 向量化服务 | ✅ | 768维向量，本地模型 |
| 查询服务 | ✅ | 关键词召回可用 |
| API 网关 | ✅ | 路由+认证+限流 |
| 数据同步 | ✅ | Kafka 消费框架 |
| OpenSearch | ✅ | 集群运行中 |
| 监控 | ✅ | Prometheus + Grafana |

### 测试状态

| 类型 | 数量 | 状态 |
|------|------|------|
| 单元测试 | 112+ | ✅ 通过 |
| 业务功能测试 | 6/10 | ⚠️ 部分完成 |
| API 网关路由 | 3/3 | ✅ 通过 |

### 已知问题

| 问题 | 影响 | 优先级 |
|------|------|--------|
| 索引需手动创建 | 需手动操作 | P1 |
| 向量召回未测试 | 功能已实现 | P1 |
| IK 分词器未安装 | 中文分词效果 | P2 |
| 数据同步未验证 | 端到端未验证 | P0 |

---

## 四、开发约定

### 工作流程

```
1. 开始新任务前 → 先阅读 docs/plans/2025-02-05-future-roadmap.md
2. 严格按照计划中的 Task 和 Files 执行
3. 保持代码风格和架构一致性
4. 完成后更新文档并 git commit
```

### 代码规范

```java
// 包命名
com.search.{service}.{layer}

// 例如：
com.search.config.admin.controller.*   // Controller
com.search.config.admin.service.*       // Service
com.search.query.recall.*               // 召回逻辑
```

### Git 提交规范

```
type: subject

类型:
- feat: 新功能
- fix: 修复 bug
- docs: 文档更新
- test: 测试相关
- refactor: 重构
- chore: 构建/工具

示例:
feat: add auto index creation for SearchObject
fix: API Gateway routing with direct URLs
docs: update roadmap with Phase 7.5
```

---

## 五、环境信息

### 远程服务器

| 项目 | 值 |
|------|-----|
| 地址 | `ubuntu@129.226.60.225` |
| SSH 密钥 | `deployments/pwj.pem` |
| 项目路径 | `~/search-platform-spec/` |

### 端口映射

| 服务 | 内部端口 | 外部端口 |
|------|---------|---------|
| Config Admin | 8080 | 8080 |
| Query Service | 8082 | 8082 |
| Vector Service | 8083 | 8083 |
| API Gateway | 8084 | 8084 |
| OpenSearch | 9200 | 9200 |
| Kafka | 29092 | 9092 |
| Prometheus | 9090 | 9090 |
| Grafana | 3000 | 3000 |

### API 认证

```
X-App-Key: test_app
X-App-Secret: test_secret
```

---

## 六、下一步计划

### 当前阶段: Phase 7.5 - 基线验证

| Task | 描述 | 优先级 |
|------|------|--------|
| 16.1 | 数据同步完整流程验证 | P0 |
| 16.2 | 向量召回端到端测试 | P0 |
| 16.3 | 索引自动创建实现 | P0 |
| 16.4 | 多路融合召回验证 | P0 |
| 16.5 | IK 中文分词器安装 | P0 |

### 后续阶段

- **Phase 8**: 生产就绪优化 (持久化队列、配置中心、监控)
- **Phase 9**: 搜索能力增强 (同义词、拼写纠错、缓存)
- **Phase 10**: 高级功能 (A/B 测试、分析报表、个性化)
- **Phase 11**: 运维增强 (索引生命周期、数据一致性、灰度发布)

详细计划见: `docs/plans/2025-02-05-future-roadmap.md`

---

## 七、重要提醒

### ⚠️ 对 AI 助手的约定

> "后续所有整个阶段的完成，都能够参照一下之前的计划，不能遗忘了之前的计划和设计"

**执行规则**:
1. 开始任何任务前，先检查 `docs/plans/2025-02-05-future-roadmap.md`
2. 严格按计划中的 Task 和 Files 执行
3. 不遗漏之前设计的功能点
4. 保持架构和代码风格一致性

### 🔍 快速检查命令

```bash
# 查看开发计划
cat docs/plans/2025-02-05-future-roadmap.md

# 查看测试报告
cat BUSINESS_FUNCTION_TEST_REPORT_*.md

# 查看服务状态
ssh -i deployments/pwj.pem ubuntu@129.226.60.225 "docker ps"

# 查看最近提交
git log --oneline -10
```

---

## 八、故障排查

### 常见问题

| 问题 | 解决方案 |
|------|---------|
| API Gateway 503 | 检查路由配置，使用直接 URL 非 lb:// |
| API Gateway 401 | 添加 X-App-Key 和 X-App-Secret 头部 |
| OpenSearch 连接失败 | 通过 SSH 在服务器本地测试 |
| 向量搜索无结果 | 确认文档有向量字段数据 |
| 索引未自动创建 | 目前需要手动创建 |

### 日志查看

```bash
# 服务日志
ssh ubuntu@129.226.60.225 "docker logs <service-name> --tail 50"

# 所有服务状态
ssh ubuntu@129.226.60.225 "docker ps --format 'table {{.Names}}\t{{.Status}}'"
```

---

**文档版本**: 1.0
**维护者**: AI Assistant + 用户
