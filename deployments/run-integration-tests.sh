#!/bin/bash

################################################################################
# 企业搜索中台 - 自动化集成测试脚本
# 用法: ./run-integration-tests.sh ubuntu@129.226.60.225
#
# 测试内容:
#   1. 基础设施健康检查 (OpenSearch, Kafka)
#   2. 各服务健康检查
#   3. API 功能测试
#   4. 数据同步流程测试
#   5. 查询服务测试
################################################################################

set -e

# ==================== 配置区域 ====================
REMOTE_HOST="${1:-ubuntu@129.226.60.225}"
REMOTE_IP="${REMOTE_HOST##*@}"

# 测试配置
TIMEOUT=30
TEST_RESULTS=()
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
# =================================================

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

clear
echo "========================================"
echo "🧪 企业搜索中台 - 自动化集成测试"
echo "========================================"
echo "测试目标:  $REMOTE_HOST"
echo "开始时间:  $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"
echo ""

# ==================== 辅助函数 ====================

# 执行远程命令
remote_exec() {
    ssh -o ConnectTimeout=$TIMEOUT -o StrictHostKeyChecking=no "$REMOTE_HOST" "$1" 2>/dev/null
}

# 本地 HTTP 请求
http_request() {
    local url=$1
    local expected=${2:-200}
    local response=$(curl -s -w "\n%{http_code}" -o /tmp/http_response_$$.txt "$url" 2>/dev/null)
    local status=$(echo "$response" | tail -1)
    local body=$(cat /tmp/http_response_$$.txt 2>/dev/null)
    rm -f /tmp/http_response_$$.txt
    echo "$status|$body"
}

# 测试函数
test_case() {
    local name="$1"
    local command="$2"
    local expected="$3"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -n "  测试 $TOTAL_TESTS: $name ... "

    if eval "$command" > /tmp/test_output_$$.txt 2>&1; then
        local output=$(cat /tmp/test_output_$$.txt)
        rm -f /tmp/test_output_$$.txt

        if [ -n "$expected" ]; then
            if echo "$output" | grep -q "$expected"; then
                echo -e "${GREEN}✓ PASS${NC}"
                PASSED_TESTS=$((PASSED_TESTS + 1))
                TEST_RESULTS+=("[$name] PASS")
            else
                echo -e "${RED}✗ FAIL${NC} (期望: $expected)"
                echo "    输出: $output"
                FAILED_TESTS=$((FAILED_TESTS + 1))
                TEST_RESULTS+=("[$name] FAIL - 期望: $expected")
            fi
        else
            echo -e "${GREEN}✓ PASS${NC}"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            TEST_RESULTS+=("[$name] PASS")
        fi
    else
        local output=$(cat /tmp/test_output_$$.txt 2>/dev/null)
        rm -f /tmp/test_output_$$.txt
        echo -e "${RED}✗ FAIL${NC}"
        echo "    错误: $output"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TEST_RESULTS+=("[$name] FAIL - $output")
    fi
}

# ==================== 测试套件 ====================

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}【第一组】基础设施健康检查${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 测试 1: SSH 连接
test_case "SSH 连接" "remote_exec 'echo OK'" "OK"

# 测试 2: Docker 运行
test_case "Docker 运行" "remote_exec 'docker --version'"

# 测试 3: OpenSearch 集群健康
test_case "OpenSearch 集群健康" "http_request http://$REMOTE_IP:9200/_cluster/health" '"green"|"yellow"'

# 测试 4: Kafka 连接
test_case "Kafka 端口监听" "remote_exec 'ss -tuln | grep :9092'" "LISTEN"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}【第二组】应用服务健康检查${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 测试 5: Config Admin
test_case "Config Admin 健康" "http_request http://$REMOTE_IP:8080/actuator/health" '"status":"UP"'

# 测试 6: Query Service
test_case "Query Service 健康" "http_request http://$REMOTE_IP:8082/actuator/health" '"status":"UP"'

# 测试 7: Vector Service
test_case "Vector Service 健康" "http_request http://$REMOTE_IP:8083/actuator/health" '"status":"UP"'

# 测试 8: API Gateway
test_case "API Gateway 健康" "http_request http://$REMOTE_IP:8084/actuator/health" '"status":"UP"'

# 测试 9: Prometheus
test_case "Prometheus 端点" "http_request http://$REMOTE_IP:9090/-/healthy" "Prometheus"

# 测试 10: Grafana
test_case "Grafana 端点" "http_request http://$REMOTE_IP:3000/api/health" '"database":"ok"'

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}【第三组】API 功能测试${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 测试 11: OpenSearch 索引列表
test_case "OpenSearch 索引 API" "http_request http://$REMOTE_IP:9200/_cat/indices?v"

# 测试 12: Kafka Topic 列表
test_case "Kafka Topic 列表" "remote_exec 'docker exec kafka kafka-topics --bootstrap-server localhost:29092 --list'"

# 测试 13: Config Admin API - 获取 Sources
test_case "Config Admin Sources API" "http_request http://$REMOTE_IP:8080/api/v1/sources"

# 测试 14: Config Admin API - 获取 Objects
test_case "Config Admin Objects API" "http_request http://$REMOTE_IP:8080/api/v1/objects"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}【第四组】容器状态检查${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

EXPECTED_CONTAINERS=(
    "opensearch-node1"
    "kafka"
    "config-admin"
    "query-service"
    "data-sync"
    "vector-service"
    "api-gateway"
)

for container in "${EXPECTED_CONTAINERS[@]}"; do
    test_case "容器运行: $container" "remote_exec 'docker ps --format \"{{.Names}}\" | grep -q $container'"
done

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}【第五组】资源使用检查${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 获取资源使用情况
echo "  服务器资源使用:"
remote_exec 'echo "    CPU: $(nproc) 核"; echo "    内存: $(free -h | grep Mem | awk "{print $3 "/" $2}")"; echo "    磁盘: $(df -h / | tail -1 | awk "{print $3 "/" $2}")"'

echo ""
echo "  容器资源使用:"
remote_exec 'docker stats --no-stream --format "table {{.Name}}\t{{.CPUSlice}}\t{{.MemUsage}}" 2>/dev/null | head -10'

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}【第六组】日志采样检查${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo "  检查最近的错误日志..."

# 检查各服务最近日志
for service in config-admin query-service vector-service api-gateway; do
    echo "  $service 最近日志:"
    remote_exec "docker logs --tail 5 $service 2>&1 | grep -i error || echo '    (无错误)'"
    echo ""
done

# ==================== 测试结果汇总 ====================
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}📊 测试结果汇总${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "总测试数: $TOTAL_TESTS"
echo -e "通过: ${GREEN}$PASSED_TESTS${NC}"
echo -e "失败: ${RED}$FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✓ 所有测试通过！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "系统状态: 健康"
    echo "可以开始使用企业搜索中台！"
    echo ""
    echo "访问地址:"
    echo "  - OpenSearch Dashboards:  http://$REMOTE_IP:5601"
    echo "  - API Gateway:           http://$REMOTE_IP:8084"
    echo "  - Grafana 监控:          http://$REMOTE_IP:3000"
    exit 0
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}✗ 有 $FAILED_TESTS 个测试失败${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    echo "失败的测试:"
    for result in "${TEST_RESULTS[@]}"; do
        if echo "$result" | grep -q FAIL; then
            echo -e "  ${RED}✗${NC} $result"
        fi
    done
    echo ""
    echo "建议:"
    echo "  1. 查看服务日志: ssh $REMOTE_HOST 'cd ~/search-platform-spec/deployments/docker && docker compose logs -f'"
    echo "  2. 重启失败的服务: ssh $REMOTE_HOST 'cd ~/search-platform-spec/deployments/docker && docker compose restart'"
    echo "  3. 检查容器状态: ssh $REMOTE_HOST 'docker ps -a'"
    exit 1
fi
