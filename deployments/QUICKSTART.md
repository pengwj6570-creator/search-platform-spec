# 远程服务器部署快速入门

> 完整的 SSH 密钥配置 + 部署流程指南

---

## 📌 部署流程总览

```
1️⃣ 配置 SSH 密钥认证
   ↓
2️⃣ 检查远程服务器环境
   ↓
3️⃣ 本地编译项目
   ↓
4️⃣ 同步并部署到远程服务器
   ↓
5️⃣ 验证服务运行状态
```

---

## 1️⃣ 配置 SSH 密钥认证

### 自动配置（推荐）

```bash
cd D:/dev/claudecode/search-platform-spec/deployments
bash setup-ssh-key.sh root@192.168.1.100
```

首次运行需要输入一次远程服务器密码。

### 验证配置

```bash
ssh root@192.168.1.100
# 应该直接登录，无需密码
```

---

## 2️⃣ 检查远程服务器环境

```bash
cd deployments
bash check-remote.sh root@192.168.1.100
```

**期望输出：**
```
========================================
🔍 远程服务器环境检查
========================================
主机: root@192.168.1.100
========================================

系统信息
  操作系统: Ubuntu 22.04.3 LTS
  内核版本: 5.15.0-72-generic
  架构:     x86_64

硬件资源
  CPU 核心数: 4
  内存总量:   16Gi
  可用内存:   12Gi
  磁盘使用:   45Gi / 100Gi (45%)

软件版本
  Docker:    ✓ Docker version 24.0.7
  Compose v2: ✓ Docker Compose version v2.23.0
  Git:       ✓ git version 2.34.1

端口占用检查
  9200 (OpenSearch): ✓ 可用
  9092 (Kafka):        ✓ 可用
  ...

========================================
✓ 环境检查通过，可以部署
========================================
```

---

## 3️⃣ 本地编译项目

```bash
cd D:/dev/claudecode/search-platform-spec

# 设置环境变量
export JAVA_HOME="/c/Program Files/Java/jdk-25.0.2"
export PATH="/c/Users/40912/maven/apache-maven-3.9.12/bin:$JAVA_HOME/bin:$PATH"

# 编译项目（跳过测试，加快速度）
mvn clean package -DskipTests
```

**预计耗时：** 3-5 分钟

**成功标志：**
```
[INFO] BUILD SUCCESS
[INFO] Total time:  XX:XX min
```

---

## 4️⃣ 同步并部署到远程服务器

### 方式 A: 一键部署（最简单）

```bash
cd deployments
bash deploy-one-click.sh root@192.168.1.100
```

### 方式 B: 分步部署

```bash
# 设置环境变量
export GIT_REPO=https://github.com/pengwj6570-creator/search-platform-spec.git

# 执行本地构建 + 同步部署
bash build-local-sync-remote.sh root@192.168.1.100
```

---

## 5️⃣ 验证服务运行状态

### 检查容器状态

```bash
ssh root@192.168.1.100 'cd ~/search-platform-spec/deployments/docker && docker compose ps'
```

**期望输出：**
```
NAME                    STATUS    PORTS
opensearch-node1        Up        0.0.0.0:9200->9200, 9600
opensearch-node2        Up
kafka                   Up        0.0.0.0:9092->9092
config-admin            Up        0.0.0.0:8080->8080
query-service           Up        0.0.0.0:8082->8082
vector-service          Up        0.0.0.0:8083->8083
api-gateway             Up        0.0.0.0:8084->8084
prometheus              Up        0.0.0.0:9090->9090
grafana                 Up        0.0.0.0:3000->3000
```

### 健康检查

```bash
# OpenSearch 集群健康
curl http://192.168.1.100:9200/_cluster/health

# 各服务健康端点
curl http://192.168.1.100:8080/actuator/health   # Config Admin
curl http://192.168.1.100:8082/actuator/health   # Query Service
curl http://192.168.1.100:8084/actuator/health   # API Gateway
```

### 访问 Web 界面

| 服务 | URL | 用户名/密码 |
|------|-----|-----------|
| OpenSearch Dashboards | http://192.168.1.100:5601 | - |
| Grafana | http://192.168.1.100:3000 | admin/admin |
| API Gateway | http://192.168.1.100:8084 | - |

---

## 📝 快速命令参考

### 常用命令

```bash
# 查看服务日志
ssh root@192.168.1.100 'cd ~/search-platform-spec/deployments/docker && docker compose logs -f'

# 查看特定服务日志
ssh root@192.168.1.100 'cd ~/search-platform-spec/deployments/docker && docker compose logs -f query-service'

# 重启所有服务
ssh root@192.168.1.100 'cd ~/search-platform-spec/deployments/docker && docker compose restart'

# 停止所有服务
ssh root@192.168.1.100 'cd ~/search-platform-spec/deployments/docker && docker compose down'

# 重新部署
cd deployments && bash deploy-one-click.sh root@192.168.1.100
```

---

## ⚠️ 常见问题

### Q1: SSH 连接超时

```bash
# 检查网络连通性
ping 192.168.1.100

# 检查 SSH 端口
telnet 192.168.1.100 22
```

### Q2: 端口被占用

```bash
# 登录服务器检查
ssh root@192.168.1.100

# 查看端口占用
lsof -i :9200

# 停止占用端口的进程
sudo kill <PID>
```

### Q3: Docker 服务未启动

```bash
# 登录服务器启动 Docker
ssh root@192.168.1.100
sudo systemctl start docker
sudo systemctl enable docker
```

### Q4: 服务启动失败

```bash
# 查看详细日志
ssh root@192.168.1.100 'cd ~/search-platform-spec/deployments/docker && docker compose logs'
```

---

## 📚 相关文档

| 文档 | 路径 |
|------|------|
| SSH 密钥配置详解 | `docs/deployment/SSH_KEY_SETUP.md` |
| 完整部署指南 | `deployments/REMOTE_DEPLOYMENT.md` |
| 项目设计文档 | `docs/plans/2025-02-04-enterprise-search-platform-design.md` |

---

## ✅ 部署检查清单

部署完成后，请确认以下项目：

- [ ] SSH 密钥登录配置完成
- [ ] 服务器环境检查通过
- [ ] 本地编译成功
- [ ] 文件同步到远程服务器
- [ ] Docker 容器全部启动
- [ ] OpenSearch 集群健康 (green)
- [ ] 各服务健康检查通过
- [ ] 可以访问 OpenSearch Dashboards
- [ ] 可以访问 Grafana 监控面板
