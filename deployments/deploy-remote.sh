#!/bin/bash

################################################################################
# 企业搜索中台 - 远程服务器自动部署脚本
# 用法: ./deploy-remote.sh [remote_user@remote_host]
################################################################################

set -e  # 遇到错误立即退出

# ==================== 配置区域 ====================
REMOTE_HOST="${1:-}"
PROJECT_DIR="search-platform-spec"
GIT_REPO="${GIT_REPO:-}"  # 通过环境变量设置 Git 仓库地址
BRANCH="${BRANCH:-master}"
# =================================================

if [ -z "$REMOTE_HOST" ]; then
    echo "❌ 错误: 请提供远程服务器地址"
    echo ""
    echo "用法: $0 [user@host]"
    echo ""
    echo "示例:"
    echo "  $0 root@192.168.1.100"
    echo "  $0 ubuntu@example.com"
    echo ""
    echo "环境变量:"
    echo "  GIT_REPO   - Git 仓库地址"
    echo "  BRANCH     - 分支名 (默认: master)"
    exit 1
fi

echo "========================================"
echo "🚀 企业搜索中台 - 远程部署"
echo "========================================"
echo "远程主机: $REMOTE_HOST"
echo "项目目录: $PROJECT_DIR"
echo "Git分支: $BRANCH"
echo "========================================"
echo ""

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ==================== 步骤 1: 检查本地 SSH 连接 ====================
echo -e "${YELLOW}[1/7]${NC} 检查 SSH 连接..."

if ! ssh -o ConnectTimeout=5 "$REMOTE_HOST" "echo 'SSH连接成功'" 2>/dev/null; then
    echo -e "${RED}❌ 无法连接到 $REMOTE_HOST${NC}"
    echo ""
    echo "请检查:"
    echo "  1. 服务器地址是否正确"
    echo "  2. SSH 服务是否运行"
    echo "  3. 是否配置了密钥或密码认证"
    exit 1
fi

echo -e "${GREEN}✓ SSH 连接正常${NC}"
echo ""

# ==================== 步骤 2: 检查远程服务器环境 ====================
echo -e "${YELLOW}[2/7]${NC} 检查远程服务器环境..."

ssh "$REMOTE_HOST" '
    echo "--- 检查 Docker ---"
    if ! command -v docker &> /dev/null; then
        echo "❌ Docker 未安装"
        exit 1
    fi
    docker --version

    echo ""
    echo "--- 检查 Docker Compose ---"
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        echo "❌ Docker Compose 未安装"
        exit 1
    fi

    # 优先使用 docker compose (v2)，回退到 docker-compose (v1)
    if docker compose version &> /dev/null; then
        echo "DOCKER_COMPOSE_CMD=docker compose" >> ~/.bash_profile
    else
        echo "DOCKER_COMPOSE_CMD=docker-compose" >> ~/.bash_profile
    fi

    echo ""
    echo "--- 检查 Git ---"
    if ! command -v git &> /dev/null; then
        echo "❌ Git 未安装"
        exit 1
    fi
    git --version

    echo ""
    echo "--- 检查端口占用 ---"
    PORTS=(9200 9092 8080 8082 8083 8084 5601 9090 3000)
    BUSY_PORTS=()
    for port in "${PORTS[@]}"; do
        if lsof -i :"$port" &> /dev/null || ss -tuln | grep -q ":$port "; then
            BUSY_PORTS+=("$port")
        fi
    done

    if [ ${#BUSY_PORTS[@]} -gt 0 ]; then
        echo "⚠️  以下端口已被占用: ${BUSY_PORTS[*]}"
        echo "   部署前请先释放这些端口"
    else
        echo "✓ 所有所需端口均可用"
    fi
'

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 环境检查失败${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 环境检查通过${NC}"
echo ""

# ==================== 步骤 3: 检查/设置 Git 仓库 ====================
echo -e "${YELLOW}[3/7]${NC} 检查 Git 仓库配置..."

if [ -z "$GIT_REPO" ]; then
    echo -e "${YELLOW}⚠️  未设置 GIT_REPO 环境变量${NC}"
    echo ""
    echo "请设置 Git 仓库地址并重新运行:"
    echo ""
    echo "  export GIT_REPO=https://github.com/yourusername/search-platform-spec.git"
    echo "  export GIT_REPO=git@github.com:yourusername/search-platform-spec.git"
    echo "  $0 $REMOTE_HOST"
    echo ""
    read -p "是否使用当前目录作为源? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
    USE_LOCAL_SOURCE=true
else
    USE_LOCAL_SOURCE=false
fi

echo ""

# ==================== 步骤 4: 同步代码到远程服务器 ====================
echo -e "${YELLOW}[4/7]${NC} 同步代码到远程服务器..."

if [ "$USE_LOCAL_SOURCE" = true ]; then
    echo "使用当前目录作为源..."
    echo "正在上传项目文件..."

    # 排除不必要的文件
    rsync -avz --progress \
        --exclude='.git' \
        --exclude='target' \
        --exclude='node_modules' \
        --exclude='.idea' \
        --exclude='.DS_Store' \
        --exclude='*.log' \
        ./ "$REMOTE_HOST:~/$PROJECT_DIR/"
else
    echo "使用 Git 仓库: $GIT_REPO"

    ssh "$REMOTE_HOST" "
        if [ -d ~/$PROJECT_DIR ]; then
            echo '更新现有仓库...'
            cd ~/$PROJECT_DIR
            git fetch origin
            git reset --hard origin/$BRANCH
        else
            echo '克隆新仓库...'
            git clone -b $BRANCH --single-branch $GIT_REPO ~/$PROJECT_DIR
        fi
    "
fi

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 代码同步失败${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 代码同步完成${NC}"
echo ""

# ==================== 步骤 5: 创建 Dockerfile ====================
echo -e "${YELLOW}[5/7]${NC} 创建服务 Dockerfile..."

# 为每个服务创建 Dockerfile
SERVICES=("config-admin" "query-service" "data-sync" "vector-service" "api-gateway")

for service in "${SERVICES[@]}"; do
    ssh "$REMOTE_HOST" "
        if [ ! -f ~/$PROJECT_DIR/services/$service/Dockerfile ]; then
            echo '创建 $service/Dockerfile...'
            cat > ~/$PROJECT_DIR/services/$service/Dockerfile << 'EOF'
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
        fi
    "
done

echo -e "${GREEN}✓ Dockerfile 准备完成${NC}"
echo ""

# ==================== 步骤 6: 构建服务 JAR 包 ====================
echo -e "${YELLOW}[6/7]${NC} 构建服务 JAR 包..."

ssh "$REMOTE_HOST" "
    cd ~/$PROJECT_DIR

    # 检查是否需要安装 Maven
    if ! command -v mvn &> /dev/null; then
        echo '安装 Maven...'
        sudo apt-get update || sudo yum update -y
        sudo apt-get install -y maven || sudo yum install -y maven
    fi

    # 构建所有模块
    echo '构建项目...'
    mvn clean package -DskipTests

    # 检查构建结果
    if [ ! -f services/config-admin/target/*.jar ]; then
        echo '❌ 构建失败: 未找到 JAR 文件'
        exit 1
    fi

    echo '✓ 构建完成'
"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 构建失败${NC}"
    echo -e "${YELLOW}提示: 可以在本地构建后再部署${NC}"
    exit 1
fi

echo -e "${GREEN}✓ JAR 包构建完成${NC}"
echo ""

# ==================== 步骤 7: 启动 Docker 服务 ====================
echo -e "${YELLOW}[7/7]${NC} 启动 Docker 服务..."

ssh "$REMOTE_HOST" "
    cd ~/$PROJECT_DIR/deployments/docker

    # 停止旧服务
    echo '停止旧服务...'
    docker compose down || docker-compose down

    # 启动新服务
    echo '启动服务...'
    docker compose up -d || docker-compose up -d

    # 等待服务启动
    echo '等待服务启动...'
    sleep 30

    # 检查服务状态
    echo ''
    echo '--- 服务状态 ---'
    docker compose ps || docker-compose ps
"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}🎉 部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "服务访问地址:"
echo "  - OpenSearch Dashboards:  http://$REMOTE_HOST:5601"
echo "  - API Gateway:           http://$REMOTE_HOST:8084"
echo "  - Config Admin:          http://$REMOTE_HOST:8080"
echo "  - Query Service:         http://$REMOTE_HOST:8082"
echo "  - Vector Service:        http://$REMOTE_HOST:8083"
echo "  - Prometheus:            http://$REMOTE_HOST:9090"
echo "  - Grafana:               http://$REMOTE_HOST:3000"
echo ""
echo "查看日志:"
echo "  ssh $REMOTE_HOST 'cd ~/$PROJECT_DIR/deployments/docker && docker compose logs -f'"
echo ""
