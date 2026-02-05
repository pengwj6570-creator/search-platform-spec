#!/bin/bash

################################################################################
# 本地构建 + 同步到远程服务器脚本
# 用法: ./build-local-sync-remote.sh [user@host]
#
# 适用场景:
# - 远程服务器没有 Maven
# - 远程服务器编译资源有限
# - 需要在本地调试后再部署
################################################################################

set -e

# ==================== 配置区域 ====================
REMOTE_HOST="${1:-}"
PROJECT_DIR="search-platform-spec"
# =================================================

if [ -z "$REMOTE_HOST" ]; then
    echo "❌ 错误: 请提供远程服务器地址"
    echo ""
    echo "用法: $0 [user@host]"
    echo "示例: $0 root@192.168.1.100"
    exit 1
fi

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "========================================"
echo "🏗️  本地构建 + 远程同步部署"
echo "========================================"
echo "远程主机: $REMOTE_HOST"
echo "========================================"
echo ""

# ==================== 步骤 1: 检查本地 Maven ====================
echo -e "${YELLOW}[1/5]${NC} 检查本地构建环境..."

if ! command -v mvn &> /dev/null; then
    echo "❌ Maven 未安装，请先安装 Maven"
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "❌ Java 未安装，请先安装 Java"
    exit 1
fi

echo "  Maven: $(mvn -version | head -1)"
echo "  Java: $(java -version 2>&1 | head -1)"
echo ""

# ==================== 步骤 2: 本地编译 ====================
echo -e "${YELLOW}[2/5]${NC} 本地编译项目..."
echo "  mvn clean package -DskipTests"

mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi

echo -e "${GREEN}✓ 编译完成${NC}"
echo ""

# ==================== 步骤 3: 检查编译产物 ====================
echo -e "${YELLOW}[3/5]${NC} 检查编译产物..."

SERVICES=("config-admin" "query-service" "data-sync" "vector-service" "api-gateway")

for service in "${SERVICES[@]}"; do
    JAR_FILE="services/$service/target/*.jar"

    if ls $JAR_FILE 1> /dev/null 2>&1; then
        echo "  ✓ $service: $(ls $JAR_FILE)"
    else
        echo "  ✗ $service: 未找到 JAR 文件"
        exit 1
    fi
done

echo ""

# ==================== 步骤 4: 创建 Dockerfile ====================
echo -e "${YELLOW}[4/5]${NC} 创建 Dockerfile..."

for service in "${SERVICES[@]}"; do
    cat > "services/$service/Dockerfile" << 'EOF'
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
    echo "  ✓ 创建 $service/Dockerfile"
done

echo ""

# ==================== 步骤 5: 同步到远程服务器 ====================
echo -e "${YELLOW}[5/5]${NC} 同步到远程服务器..."

# 在远程创建目录结构
ssh "$REMOTE_HOST" "mkdir -p ~/$PROJECT_DIR/deployments/docker"

# 同步项目文件 (排除不需要的)
echo "  同步项目文件..."
rsync -avz --progress \
    --exclude='.git' \
    --exclude='node_modules' \
    --exclude='.idea' \
    --exclude='.DS_Store' \
    --exclude='*.log' \
    ./ "$REMOTE_HOST:~/$PROJECT_DIR/"

# 同步编译后的 JAR 文件
echo "  同步 JAR 文件..."
for service in "${SERVICES[@]}"; do
    echo "    - $service"
    rsync -avz "services/$service/target/"*.jar \
        "$REMOTE_HOST:~/$PROJECT_DIR/services/$service/target/"
done

echo ""

# ==================== 步骤 6: 启动服务 ====================
echo -e "${YELLOW}[6/6]${NC} 启动 Docker 服务..."

ssh "$REMOTE_HOST" "
    cd ~/$PROJECT_DIR/deployments/docker

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
echo "服务访问地址:"
echo "  - OpenSearch Dashboards:  http://$(echo $REMOTE_HOST | cut -d@ -f2):5601"
echo "  - API Gateway:           http://$(echo $REMOTE_HOST | cut -d@ -f2):8084"
echo "  - Config Admin:          http://$(echo $REMOTE_HOST | cut -d@ -f2):8080"
echo "  - Query Service:         http://$(echo $REMOTE_HOST | cut -d@ -f2):8082"
echo "  - Vector Service:        http://$(echo $REMOTE_HOST | cut -d@ -f2):8083"
echo ""
