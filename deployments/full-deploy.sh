#!/bin/bash

################################################################################
# 企业搜索中台 - 完整自动化部署脚本
# 用法: ./full-deploy.sh ubuntu@129.226.60.225
#
# 注意: 首次运行需要输入一次密码，之后将使用密钥认证
################################################################################

set -e

# ==================== 配置区域 ====================
REMOTE_HOST="${1:-ubuntu@129.226.60.225}"

# 自动检测环境
export JAVA_HOME="/c/Program Files/Java/jdk-25.0.2"
export MAVEN_HOME="/c/Users/40912/maven/apache-maven-3.9.12"
export PATH="$MAVEN_HOME/bin:$JAVA_HOME/bin:$PATH"
export GIT_REPO="https://github.com/pengwj6570-creator/search-platform-spec.git"
export BRANCH="master"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
# =================================================

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

clear
echo "========================================"
echo "🚀 企业搜索中台 - 完整自动化部署"
echo "========================================"
echo "远程主机:  $REMOTE_HOST"
echo "Git仓库:   $GIT_REPO"
echo "项目目录:  $PROJECT_ROOT"
echo "========================================"
echo ""

# ==================== 检查参数 ====================
if [ -z "$REMOTE_HOST" ]; then
    echo -e "${RED}错误: 请提供远程服务器地址${NC}"
    echo "用法: $0 [user@host]"
    echo "示例: $0 ubuntu@129.226.60.225"
    exit 1
fi

# 提取用户名和IP
REMOTE_USER="${REMOTE_HOST%%@*}"
REMOTE_IP="${REMOTE_HOST##*@}"

# ==================== 步骤 1: 生成 SSH 密钥对 ====================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[1/6] 生成 SSH 密钥对...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

SSH_DIR="$HOME/.ssh"
KEY_NAME="search_platform_$(date +%Y%m%d)"
PRIVATE_KEY="$SSH_DIR/${KEY_NAME}"
PUBLIC_KEY="${PRIVATE_KEY}.pub"

# 检查是否已存在密钥
if [ -f "$PRIVATE_KEY" ]; then
    echo "  检测到已存在的密钥: $PRIVATE_KEY"
    read -p "  是否使用现有密钥? (Y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Nn]$ ]]; then
        echo "  使用现有密钥"
    else
        rm -f "$PRIVATE_KEY" "$PUBLIC_KEY"
        CREATE_KEY=true
    fi
else
    CREATE_KEY=true
fi

if [ "$CREATE_KEY" = true ]; then
    echo "  正在生成新密钥..."
    if [ ! -d "$SSH_DIR" ]; then
        mkdir -p "$SSH_DIR"
        chmod 700 "$SSH_DIR"
    fi

    ssh-keygen -t rsa -b 4096 -f "$PRIVATE_KEY" -N "" -C "search-platform@$(hostname)" 2>/dev/null
    echo -e "${GREEN}  ✓ 密钥生成成功${NC}"
    echo "    私钥: $PRIVATE_KEY"
    echo "    公钥: $PUBLIC_KEY"
fi

echo ""

# ==================== 步骤 2: 配置 SSH 客户端 ====================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[2/6] 配置 SSH 客户端...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 更新 SSH 配置
CONFIG_FILE="$SSH_DIR/config"
if [ -f "$CONFIG_FILE" ]; then
    cp "$CONFIG_FILE" "${CONFIG_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
    echo "  已备份原配置"
fi

# 添加配置（去重）
grep -v "Host $REMOTE_IP" "$CONFIG_FILE" > /tmp/ssh_config.tmp 2>/dev/null || touch /tmp/ssh_config.tmp
cat >> /tmp/ssh_config.tmp << EOF

# Search Platform - Auto-generated
Host $REMOTE_IP
    HostName $REMOTE_IP
    User $REMOTE_USER
    IdentityFile $PRIVATE_KEY
    IdentitiesOnly yes
    ServerAliveInterval 60
    ServerAliveCountMax 3
EOF
mv /tmp/ssh_config.tmp "$CONFIG_FILE"

echo -e "${GREEN}  ✓ SSH 配置已更新${NC}"
echo ""

# ==================== 步骤 3: 复制公钥到远程服务器 ====================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[3/6] 复制公钥到远程服务器...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo "  正在复制公钥..."
echo "  ⚠️  需要输入一次密码"
echo ""

# 尝试使用 ssh-copy-id
if ssh-copy-id -i "$PUBLIC_KEY" "$REMOTE_HOST" 2>/dev/null; then
    echo -e "${GREEN}  ✓ 公钥已复制到服务器${NC}"
elif command -v sshpass &> /dev/null; then
    echo "  ssh-copy-id 不可用，尝试手动方式..."
    read -s -p "  请输入 $REMOTE_HOST 的密码: " PASSWORD
    echo ""

    # 获取公钥内容
    PUB_KEY_CONTENT=$(cat "$PUBLIC_KEY")

    # 通过 SSH 添加公钥
    ssh "$REMOTE_HOST" "
        mkdir -p ~/.ssh
        chmod 700 ~/.ssh
        echo '$PUB_KEY_CONTENT' >> ~/.ssh/authorized_keys
        chmod 600 ~/.ssh/authorized_keys
        echo '公钥已添加'
    " 2>/dev/null

    unset PASSWORD
    echo -e "${GREEN}  ✓ 公钥已复制到服务器${NC}"
else
    echo -e "${RED}  ✗ 公钥复制失败${NC}"
    echo ""
    echo "请手动执行以下步骤:"
    echo ""
    echo "1. 复制以下公钥:"
    cat "$PUBLIC_KEY"
    echo ""
    echo "2. 登录服务器并添加公钥:"
    echo "   ssh $REMOTE_HOST"
    echo "   mkdir -p ~/.ssh"
    echo "   chmod 700 ~/.ssh"
    echo "   echo '$(cat $PUBLIC_KEY)' >> ~/.ssh/authorized_keys"
    echo "   chmod 600 ~/.ssh/authorized_keys"
    echo ""
    read -p "按回车继续..."
fi

echo ""

# ==================== 步骤 4: 测试 SSH 连接 ====================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[4/6] 测试 SSH 连接...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if ssh -o ConnectTimeout=10 -o StrictHostKeyChecking=no "$REMOTE_HOST" "echo '连接成功!'" 2>/dev/null; then
    echo -e "${GREEN}  ✓ SSH 连接测试成功${NC}"
    echo -e "${GREEN}  ✓ 密钥认证工作正常${NC}"
else
    echo -e "${RED}  ✗ SSH 连接失败${NC}"
    echo ""
    echo "请检查:"
    echo "  1. 服务器地址是否正确"
    echo "  2. SSH 服务是否运行"
    echo "  3. 密钥是否正确添加"
    exit 1
fi

echo ""

# ==================== 步骤 5: 检查远程环境 ====================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[5/6] 检查远程服务器环境...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

ENV_OK=true

# 检查 Docker
if ssh "$REMOTE_HOST" "command -v docker" 2>/dev/null; then
    DOCKER_VERSION=$(ssh "$REMOTE_HOST" "docker --version")
    echo -e "  Docker:    ${GREEN}✓${NC} $DOCKER_VERSION"
else
    echo -e "  Docker:    ${RED}✗ 未安装${NC}"
    echo -e "  ${YELLOW}  正在自动安装 Docker...${NC}"
    ssh "$REMOTE_HOST" "
        if [ -f /etc/debian_version ]; then
            curl -fsSL https://get.docker.com | sh
            sudo usermod -aG docker ubuntu
        elif [ -f /etc/redhat-release ]; then
            sudo yum install -y docker
            sudo systemctl start docker
            sudo systemctl enable docker
            sudo usermod -aG docker ubuntu
        fi
    " 2>/dev/null
    echo -e "  ${GREEN}  ✓ Docker 已安装${NC}"
fi

# 检查 Docker Compose
if ssh "$REMOTE_HOST" "docker compose version" 2>/dev/null; then
    COMPOSE_VERSION=$(ssh "$REMOTE_HOST" "docker compose version")
    echo -e "  Compose:   ${GREEN}✓${NC} $COMPOSE_VERSION"
elif ssh "$REMOTE_HOST" "docker-compose --version" 2>/dev/null; then
    COMPOSE_VERSION=$(ssh "$REMOTE_HOST" "docker-compose --version")
    echo -e "  Compose:   ${GREEN}✓${NC} $COMPOSE_VERSION"
else
    echo -e "  Compose:   ${RED}✗ 未安装${NC}"
    ENV_OK=false
fi

# 检查 Git
if ssh "$REMOTE_HOST" "command -v git" 2>/dev/null; then
    GIT_VERSION=$(ssh "$REMOTE_HOST" "git --version")
    echo -e "  Git:       ${GREEN}✓${NC} $GIT_VERSION"
else
    echo -e "  Git:       ${RED}✗ 未安装${NC}"
    ENV_OK=false
fi

# 检查端口
echo ""
echo "  检查端口占用:"
PORTS=(9200 9092 8080 8082 8083 8084 5601 9090 3000)
BUSY_PORTS=()
for port in "${PORTS[@]}"; do
    if ssh "$REMOTE_HOST" "lsof -i :$port 2>/dev/null | grep LISTEN || ss -tuln | grep -q ':$port '" 2>/dev/null; then
        BUSY_PORTS+=("$port")
        echo -e "    $port: ${YELLOW}● 已占用${NC}"
    else
        echo -e "    $port: ${GREEN}✓ 可用${NC}"
    fi
done

if [ ${#BUSY_PORTS[@]} -gt 0 ]; then
    echo ""
    echo -e "${YELLOW}  ⚠️  警告: 以下端口已被占用: ${BUSY_PORTS[*]}${NC}"
    read -p "  是否继续部署? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "部署已取消"
        exit 1
    fi
fi

echo ""
if [ "$ENV_OK" = true ]; then
    echo -e "${GREEN}  ✓ 环境检查通过${NC}"
else
    echo -e "${RED}  ✗ 环境不满足要求${NC}"
    exit 1
fi

echo ""

# ==================== 步骤 6: 执行部署 ====================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[6/6] 开始部署...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

cd "$PROJECT_ROOT"

# 检查是否已编译
if [ ! -f "services/config-admin/target/*.jar" ]; then
    echo "  本地编译中..."
    echo "  执行: mvn clean package -DskipTests"

    mvn clean package -DskipTests

    if [ $? -ne 0 ]; then
        echo -e "${RED}  ✗ 编译失败${NC}"
        exit 1
    fi
    echo -e "${GREEN}  ✓ 编译成功${NC}"
else
    echo "  检测到已编译的 JAR 文件"
fi

echo ""
echo "  同步文件到远程服务器..."

# 同步项目文件
rsync -avz --progress \
    --exclude='.git' \
    --exclude='node_modules' \
    --exclude='.idea' \
    --exclude='.DS_Store' \
    --exclude='*.log' \
    --exclude='target' \
    "$PROJECT_ROOT/" "$REMOTE_HOST:~/search-platform-spec/"

# 同步 JAR 文件
SERVICES=("config-admin" "query-service" "data-sync" "vector-service" "api-gateway")
for service in "${SERVICES[@]}"; do
    echo "    - $service"
    rsync -avz "$PROJECT_ROOT/services/$service/target/"*.jar \
        "$REMOTE_HOST:~/search-platform-spec/services/$service/target/"
done

echo -e "${GREEN}  ✓ 文件同步完成${NC}"
echo ""
echo "  启动 Docker 服务..."

# 在远程执行部署
ssh "$REMOTE_HOST" '
    cd ~/search-platform-spec/deployments/docker

    # 停止旧服务
    docker compose down 2>/dev/null || docker-compose down 2>/dev/null || true

    # 启动新服务
    docker compose up -d 2>/dev/null || docker-compose up -d

    # 等待服务启动
    echo "  等待服务启动..."
    sleep 30

    # 显示状态
    echo ""
    echo "--- 服务状态 ---"
    docker compose ps 2>/dev/null || docker-compose ps
'

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}🎉 部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "服务访问地址:"
echo "  - OpenSearch Dashboards:  http://$REMOTE_IP:5601"
echo "  - API Gateway:           http://$REMOTE_IP:8084"
echo "  - Config Admin:          http://$REMOTE_IP:8080"
echo "  - Query Service:         http://$REMOTE_IP:8082"
echo "  - Vector Service:        http://$REMOTE_IP:8083"
echo "  - Prometheus:            http://$REMOTE_IP:9090"
echo "  - Grafana:               http://$REMOTE_IP:3000 (admin/admin)"
echo ""
echo "常用命令:"
echo "  查看日志:  ssh $REMOTE_HOST 'cd ~/search-platform-spec/deployments/docker && docker compose logs -f'"
echo "  重启服务:  ssh $REMOTE_HOST 'cd ~/search-platform-spec/deployments/docker && docker compose restart'"
echo "  停止服务:  ssh $REMOTE_HOST 'cd ~/search-platform-spec/deployments/docker && docker compose down'"
echo ""
echo "健康检查:"
echo "  curl http://$REMOTE_IP:9200/_cluster/health"
echo "  curl http://$REMOTE_IP:8080/actuator/health"
echo ""
