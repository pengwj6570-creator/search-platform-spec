#!/bin/bash

################################################################################
# 企业搜索中台 - 服务状态检查脚本
# 用法: bash check-services.sh
#
# 在远程服务器上运行此脚本以检查所有服务状态
################################################################################

echo "========================================"
echo "🔍 企业搜索中台 - 服务状态检查"
echo "========================================"
echo "检查时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# ==================== 容器状态 ====================
echo "=== 1. 容器运行状态 ==="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "=== 2. 停止/重启的容器 ==="
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -v "Up"

echo ""
echo "=== 3. 资源使用情况 ==="
echo "内存: "
free -h
echo ""
echo "磁盘: "
df -h /

echo ""
echo "================== 健康检查 ==================="

# ==================== 基础设施健康检查 ====================
echo ""
echo "--- OpenSearch 集群健康 ---"
OPENSEARCH_HEALTH=$(curl -s http://localhost:9200/_cluster/health?pretty=true)
echo "$OPENSEARCH_HEALTH" | grep -E '"status"|"number_of_nodes"'

echo ""
echo "--- Kafka Topics ---"
docker exec kafka kafka-topics --bootstrap-server localhost:29092 --list 2>/dev/null || echo "无法连接 Kafka"

echo ""
echo "================== 应用服务健康检查 ===================="

# ==================== 应用服务健康检查 ====================
SERVICES=(
    "config-admin:8080"
    "vector-service:8083"
    "api-gateway:8084"
)

for service_port in "${SERVICES[@]}"; do
    service="${service_port%:*}"
    port="${service_port#*:}"

    echo ""
    echo "--- $service (端口 $port) ---"

    # 健康检查
    HEALTH=$(curl -s "http://localhost:$port/actuator/health" 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo "状态: $HEALTH"
    else
        echo "状态: ❌ 无法连接"
    fi

    # 最近日志
    echo "最近日志:"
    docker logs $service 2>&1 | tail -5
done

echo ""
echo "================== 访问地址 ===================="
echo ""
echo "Web 界面:"
echo "  - OpenSearch Dashboards:  http://$(hostname -I | awk '{print $1}'):5601"
echo "  - Grafana:               http://$(hostname -I | awk '{print $1}'):3000 (admin/admin)"
echo "  - Prometheus:           http://$(hostname -I | awk '{print $1}'):9090"
echo ""
echo "API 端点:"
echo "  - Config Admin:          http://$(hostname -I | awk '{print $1}'):8080"
echo "  - Vector Service:        http://$(hostname -I | awk '{print $1}'):8083"
echo "  - API Gateway:           http://$(hostname -I | awk '{print $1}'):8084"
echo "  - OpenSearch:            http://$(hostname -I | awk '{print $1}'):9200"
echo ""

echo "================== 总结 ===================="
RUNNING_COUNT=$(docker ps --format "{{.Names}}" | grep -v -E "zookeeper|kafka" | wc -l)
echo "运行中的应用服务: $RUNNING_COUNT"
echo ""

if [ "$RUNNING_COUNT" -ge 4 ]; then
    echo "✓ 所有服务运行正常"
    exit 0
else
    echo "⚠️  部分服务未运行，请检查日志"
    exit 1
fi
