# 远程服务器部署指南

## 📋 前置条件

### 远程服务器要求

| 项目 | 要求 |
|------|------|
| 操作系统 | Linux (CentOS 7+/Ubuntu 20.04+) |
| Docker | 20.10+ |
| Docker Compose | 2.0+ |
| Git | 2.x+ |
| 内存 | ≥8 GB (推荐 16 GB) |
| 磁盘 | ≥50 GB |

### 网络要求

- 本地能 SSH 连接到远程服务器
- 以下端口已开放或无占用: 9200, 9092, 8080, 8082, 8083, 8084, 5601, 9090, 3000

---

## 🔑 第一步：配置 SSH 密钥认证（推荐）

使用 SSH 密钥可以避免每次输入密码，提高安全性和便捷性。

### 自动配置（最简单）

```bash
cd deployments
bash setup-ssh-key.sh 用户名@服务器IP
```

脚本会自动：
1. 生成 SSH 密钥对
2. 复制公钥到远程服务器
3. 配置本地 SSH 客户端
4. 测试密钥登录

### 手动配置

详见 [SSH 密钥认证配置指南](../docs/deployment/SSH_KEY_SETUP.md)

### 验证配置

```bash
# 应该直接登录，无需密码
ssh 用户名@服务器IP
```

---

## 🚀 快速部署

### 方式一: 远程 Git 拉取部署

```bash
# 1. 设置 Git 仓库地址
export GIT_REPO=https://github.com/yourusername/search-platform-spec.git
export BRANCH=master

# 2. 执行部署
cd deployments
chmod +x deploy-remote.sh
./deploy-remote.sh user@your-server-ip
```

### 方式二: 本地构建 + 远程同步

```bash
# 1. 先在本地构建
cd deployments
chmod +x build-local-sync-remote.sh
./build-local-sync-remote.sh user@your-server-ip
```

**推荐方式二**，因为：
- 本地编译更快
- 可以在本地验证编译结果
- 远程服务器不需要安装 Maven

---

## 🔍 部署前检查

```bash
# 检查远程服务器环境
cd deployments
chmod +x check-remote.sh
./check-remote.sh user@your-server-ip
```

输出示例：
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
  Java:      ○ 未安装 (可选)
  Maven:     ○ 未安装 (可本地构建)

端口占用检查
  9200 (OpenSearch): ✓ 可用
  9092 (Kafka):        ✓ 可用
  8080 (Config-Admin): ✓ 可用
  ...
```

---

## 📂 部署脚本说明

| 脚本 | 用途 | 使用场景 |
|------|------|----------|
| `check-remote.sh` | 环境检查 | 部署前验证服务器配置 |
| `deploy-remote.sh` | Git拉取部署 | 远程服务器有Git和Maven |
| `build-local-sync-remote.sh` | 本地构建部署 | 本地编译，远程只运行 |

---

## 🔧 常见问题

### 1. SSH 连接失败

```
❌ 无法连接到 user@host
```

**解决方法:**
- 检查服务器地址和用户名是否正确
- 确认 SSH 服务运行中
- 配置密钥认证或准备密码

### 2. 端口被占用

```
⚠️  以下端口已被占用: 9200 9092
```

**解决方法:**
```bash
# 登录服务器释放端口
ssh user@host

# 查看占用进程
lsof -i :9200
# 或
ss -tuln | grep 9200

# 停止占用端口的进程
sudo kill <PID>
```

### 3. Docker 未安装

**Ubuntu/Debian:**
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

**CentOS/RHEL:**
```bash
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER
```

### 4. Maven 构建失败

**解决方案:** 使用本地构建方式
```bash
./build-local-sync-remote.sh user@host
```

---

## 📊 部署后验证

### 1. 检查服务状态

```bash
ssh user@host 'cd ~/search-platform-spec/deployments/docker && docker compose ps'
```

### 2. 查看服务日志

```bash
ssh user@host 'cd ~/search-platform-spec/deployments/docker && docker compose logs -f'
```

### 3. 访问 Web 界面

| 服务 | URL | 说明 |
|------|-----|------|
| OpenSearch Dashboards | http://server:5601 | 查看 ES 索引和数据 |
| Grafana | http://server:3000 | 监控面板 (admin/admin) |
| API Gateway | http://server:8084 | 统一入口 |

### 4. 健康检查

```bash
# OpenSearch
curl http://server:9200/_cluster/health

# 各服务健康端点
curl http://server:8080/actuator/health   # Config Admin
curl http://server:8082/actuator/health   # Query Service
curl http://server:8083/actuator/health   # Vector Service
curl http://server:8084/actuator/health   # API Gateway
```

---

## 🛑 停止服务

```bash
ssh user@host 'cd ~/search-platform-spec/deployments/docker && docker compose down'
```

## 🔄 重启服务

```bash
ssh user@host 'cd ~/search-platform-spec/deployments/docker && docker compose restart'
```

## 🗑️ 清理环境

```bash
ssh user@host 'cd ~/search-platform-spec/deployments/docker && docker compose down -v'
# -v 参数会删除所有数据卷，谨慎使用！
```

---

## 📝 下一步

部署完成后，请执行测试验证：

```bash
# 运行集成测试（需要实现）
./run-integration-tests.sh
```
