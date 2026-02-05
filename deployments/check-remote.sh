#!/bin/bash

################################################################################
# 远程服务器环境检查脚本
# 用法: ./check-remote.sh [user@host]
################################################################################

set -e

REMOTE_HOST="${1:-}"

if [ -z "$REMOTE_HOST" ]; then
    echo "用法: $0 [user@host]"
    echo "示例: $0 root@192.168.1.100"
    exit 1
fi

echo "========================================"
echo "🔍 远程服务器环境检查"
echo "========================================"
echo "主机: $REMOTE_HOST"
echo "========================================"
echo ""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

check_pass=true

# ==================== 系统信息 ====================
echo -e "${YELLOW}系统信息${NC}"
ssh "$REMOTE_HOST" '
    echo "  操作系统: $(cat /etc/os-release | grep PRETTY_NAME | cut -d'"'"' -f2)"
    echo "  内核版本: $(uname -r)"
    echo "  架构:     $(uname -m)"
    echo "  时间:     $(date)"
'
echo ""

# ==================== 硬件资源 ====================
echo -e "${YELLOW}硬件资源${NC}"
ssh "$REMOTE_HOST" '
    echo "  CPU 核心数: $(nproc)"
    echo "  内存总量:   $(free -h | grep Mem | awk '"'"'{print $2}'"'")"
    echo "  可用内存:   $(free -h | grep Mem | awk '"'"'{print $7}'"'")"
    echo "  磁盘使用:   $(df -h / | tail -1 | awk '"'"'{print $3 "/" $2 " (" $5 ")"}"'"'")"
'
echo ""

# ==================== 软件版本 ====================
echo -e "${YELLOW}软件版本${NC}"

# Docker
if ssh "$REMOTE_HOST" "command -v docker" 2>/dev/null; then
    DOCKER_VERSION=$(ssh "$REMOTE_HOST" "docker --version")
    echo -e "  Docker:    ${GREEN}✓${NC} $DOCKER_VERSION"
else
    echo -e "  Docker:    ${RED}✗ 未安装${NC}"
    check_pass=false
fi

# Docker Compose
if ssh "$REMOTE_HOST" "docker compose version" 2>/dev/null; then
    COMPOSE_VERSION=$(ssh "$REMOTE_HOST" "docker compose version")
    echo -e "  Compose v2: ${GREEN}✓${NC} $COMPOSE_VERSION"
elif ssh "$REMOTE_HOST" "docker-compose --version" 2>/dev/null; then
    COMPOSE_VERSION=$(ssh "$REMOTE_HOST" "docker-compose --version")
    echo -e "  Compose v1: ${GREEN}✓${NC} $COMPOSE_VERSION"
else
    echo -e "  Compose:   ${RED}✗ 未安装${NC}"
    check_pass=false
fi

# Git
if ssh "$REMOTE_HOST" "command -v git" 2>/dev/null; then
    GIT_VERSION=$(ssh "$REMOTE_HOST" "git --version")
    echo -e "  Git:       ${GREEN}✓${NC} $GIT_VERSION"
else
    echo -e "  Git:       ${RED}✗ 未安装${NC}"
    check_pass=false
fi

# Java (可选)
if ssh "$REMOTE_HOST" "command -v java" 2>/dev/null; then
    JAVA_VERSION=$(ssh "$REMOTE_HOST" "java -version 2>&1 | head -1")
    echo -e "  Java:      ${GREEN}✓${NC} $JAVA_VERSION"
else
    echo -e "  Java:      ${YELLOW}○ 未安装 (可选)${NC}"
fi

# Maven (可选)
if ssh "$REMOTE_HOST" "command -v mvn" 2>/dev/null; then
    MVN_VERSION=$(ssh "$REMOTE_HOST" "mvn -version | head -1")
    echo -e "  Maven:     ${GREEN}✓${NC} $MVN_VERSION"
else
    echo -e "  Maven:     ${YELLOW}○ 未安装 (可本地构建)${NC}"
fi

echo ""

# ==================== 端口检查 ====================
echo -e "${YELLOW}端口占用检查${NC}"

PORTS=(
    "9200:OpenSearch"
    "9092:Kafka"
    "8080:Config-Admin"
    "8082:Query-Service"
    "8083:Vector-Service"
    "8084:API-Gateway"
    "5601:OpenSearch-Dashboards"
    "9090:Prometheus"
    "3000:Grafana"
)

for port_info in "${PORTS[@]}"; do
    port="${port_info%%:*}"
    name="${port_info##*::}"

    if ssh "$REMOTE_HOST" "lsof -i :$port 2>/dev/null | grep LISTEN || ss -tuln | grep -q ':$port '" 2>/dev/null; then
        echo -e "  $port ($name): ${YELLOW}● 已占用${NC}"
    else
        echo -e "  $port ($name): ${GREEN}✓ 可用${NC}"
    fi
done

echo ""

# ==================== Docker 运行检查 ====================
echo -e "${YELLOW}Docker 状态${NC}"

if ssh "$REMOTE_HOST" "command -v docker" 2>/dev/null; then
    CONTAINER_COUNT=$(ssh "$REMOTE_HOST" "docker ps -q | wc -l" 2>/dev/null || echo "0")
    echo "  运行中容器: $CONTAINER_COUNT 个"

    # 检查是否有相关容器在运行
    RELEVANT_CONTAINERS=$(ssh "$REMOTE_HOST" "docker ps --format '{{.Names}}' | grep -E 'opensearch|kafka|search|config|query|vector|gateway'" 2>/dev/null || echo "")
    if [ -n "$RELEVANT_CONTAINERS" ]; then
        echo "  发现相关容器:"
        echo "$RELEVANT_CONTAINERS" | sed 's/^/    /'
    fi
fi

echo ""

# ==================== 防火墙检查 ====================
echo -e "${YELLOW}防火墙状态${NC}"

if ssh "$REMOTE_HOST" "command -v ufw" 2>/dev/null; then
    UFW_STATUS=$(ssh "$REMOTE_HOST" "ufw status" 2>/dev/null || echo "")
    echo "  UFW: $UFW_STATUS"
elif ssh "$REMOTE_HOST" "command -v firewall-cmd" 2>/dev/null; then
    FW_STATUS=$(ssh "$REMOTE_HOST" "firewall-cmd --state" 2>/dev/null || echo "")
    echo "  firewalld: $FW_STATUS"
elif ssh "$REMOTE_HOST" "command -v iptables" 2>/dev/null; then
    echo "  iptables: 已安装"
else
    echo "  未检测到防火墙"
fi

echo ""

# ==================== 总结 ====================
echo "========================================"
if [ "$check_pass" = true ]; then
    echo -e "${GREEN}✓ 环境检查通过，可以部署${NC}"
else
    echo -e "${RED}✗ 环境不满足要求，请先安装缺失组件${NC}"
fi
echo "========================================"
