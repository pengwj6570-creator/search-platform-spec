#!/bin/bash

################################################################################
# 企业搜索中台 - 一键部署脚本
# 用法: ./deploy-one-click.sh [user@host]
################################################################################

set -e

# ==================== 配置区域 ====================
REMOTE_HOST="${1:-}"

# 自动检测环境
export JAVA_HOME="/c/Program Files/Java/jdk-25.0.2"
export MAVEN_HOME="/c/Users/40912/maven/apache-maven-3.9.12"
export PATH="$MAVEN_HOME/bin:$JAVA_HOME/bin:$PATH"
export GIT_REPO="https://github.com/pengwj6570-creator/search-platform-spec.git"
export BRANCH="master"

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# =================================================

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# ==================== 检查参数 ====================
if [ -z "$REMOTE_HOST" ]; then
    echo "========================================"
    echo "🚀 企业搜索中台 - 一键部署"
    echo "========================================"
    echo ""
    echo "用法: $0 [user@host]"
    echo ""
    echo "示例:"
    echo "  $0 root@192.168.1.100"
    echo "  $0 ubuntu@example.com"
    echo ""
    exit 1
fi

echo "========================================"
echo "🚀 企业搜索中台 - 一键部署"
echo "========================================"
echo "远程主机:  $REMOTE_HOST"
echo "Git仓库:   $GIT_REPO"
echo "项目目录:  $PROJECT_ROOT"
echo "========================================"
echo ""

# ==================== 步骤 1: 环境检查 ====================
echo -e "${YELLOW}[1/4] 检查本地环境...${NC}"

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven 未找到${NC}"
    echo "请设置 JAVA_HOME 和 MAVEN_HOME"
    exit 1
fi
echo "  Maven: $(mvn -version | head -1)"

# 检查 Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java 未找到${NC}"
    exit 1
fi
echo "  Java: $(java -version 2>&1 | head -1)"

# 检查 SSH 连接
if ! ssh -o ConnectTimeout=5 "$REMOTE_HOST" "echo 'SSH连接成功'" 2>/dev/null; then
    echo -e "${RED}❌ 无法连接到 $REMOTE_HOST${NC}"
    exit 1
fi
echo "  SSH:   ✓ 连接正常"

echo ""

# ==================== 步骤 2: 本地构建 ====================
echo -e "${YELLOW}[2/4] 本地编译项目...${NC}"
cd "$PROJECT_ROOT"

echo "  执行: mvn clean package -DskipTests"
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 编译失败${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 编译成功${NC}"
echo ""

# ==================== 步骤 3: 同步到远程 ====================
echo -e "${YELLOW}[3/4] 同步文件到远程服务器...${NC}"

# 在远程创建目录
ssh "$REMOTE_HOST" "mkdir -p ~/$PROJECT_ROOT/deployments/docker"

# 同步项目文件
echo "  同步项目文件..."
rsync -avz --progress \
    --exclude='.git' \
    --exclude='node_modules' \
    --exclude='.idea' \
    --exclude='.DS_Store' \
    --exclude='*.log' \
    --exclude='target' \
    "$PROJECT_ROOT/" "$REMOTE_HOST:~/$PROJECT_ROOT/"

# 同步编译后的 JAR 文件
echo "  同步 JAR 文件..."
SERVICES=("config-admin" "query-service" "data-sync" "vector-service" "api-gateway")
for service in "${SERVICES[@]}"; do
    rsync -avz "$PROJECT_ROOT/services/$service/target/"*.jar \
        "$REMOTE_HOST:~/$PROJECT_ROOT/services/$service/target/"
done

# 创建 Dockerfile
for service in "${SERVICES[@]}"; do
    ssh "$REMOTE_HOST" "
        if [ ! -f ~/$PROJECT_ROOT/services/$service/Dockerfile ]; then
            cat > ~/$PROJECT_ROOT/services/$service/Dockerfile << 'EOF'
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]
EOF
        fi
    "
done

echo -e "${GREEN}✓ 同步完成${NC}"
echo ""

# ==================== 步骤 4: 启动服务 ====================
echo -e "${YELLOW}[4/4] 启动 Docker 服务...${NC}"

ssh "$REMOTE_HOST" "
    cd ~/$PROJECT_ROOT/deployments/docker

    # 停止旧服务
    echo '  停止旧服务...'
    docker compose down 2>/dev/null || docker-compose down 2>/dev/null || true

    # 启动新服务
    echo '  启动新服务...'
    docker compose up -d 2>/dev/null || docker-compose up -d

    # 等待服务启动
    echo '  等待服务启动...'
    sleep 30

    # 显示服务状态
    echo ''
    echo '--- 服务状态 ---'
    docker compose ps 2>/dev/null || docker-compose ps
"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}🎉 部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 提取服务器 IP
SERVER_IP=$(echo "$REMOTE_HOST" | cut -d@ -f2)

echo "服务访问地址:"
echo "  - OpenSearch Dashboards:  http://$SERVER_IP:5601"
echo "  - API Gateway:           http://$SERVER_IP:8084"
echo "  - Config Admin:          http://$SERVER_IP:8080"
echo "  - Query Service:         http://$SERVER_IP:8082"
echo "  - Vector Service:        http://$SERVER_IP:8083"
echo "  - Prometheus:            http://$SERVER_IP:9090"
echo "  - Grafana:               http://$SERVER_IP:3000 (admin/admin)"
echo ""
echo "查看日志:"
echo "  ssh $REMOTE_HOST 'cd ~/$PROJECT_ROOT/deployments/docker && docker compose logs -f'"
echo ""
echo "健康检查:"
echo "  curl http://$SERVER_IP:9200/_cluster/health"
echo ""
