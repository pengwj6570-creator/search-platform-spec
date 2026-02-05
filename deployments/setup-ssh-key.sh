#!/bin/bash

################################################################################
# SSH 密钥认证配置脚本
# 用法: ./setup-ssh-key.sh [user@remote-host]
################################################################################

set -e

REMOTE_HOST="${1:-}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "========================================"
echo "🔑 SSH 密钥认证配置"
echo "========================================"
echo ""

if [ -z "$REMOTE_HOST" ]; then
    echo "用法: $0 [user@host]"
    echo ""
    echo "示例:"
    echo "  $0 root@192.168.1.100"
    echo "  $0 ubuntu@example.com"
    exit 1
fi

# 提取用户名和主机
REMOTE_USER="${REMOTE_HOST%%@*}"
REMOTE_IP="${REMOTE_HOST##*@}"

echo "远程主机: $REMOTE_HOST"
echo "  用户: $REMOTE_USER"
echo "  IP:   $REMOTE_IP"
echo ""

# ==================== 步骤 1: 生成 SSH 密钥对 ====================
echo -e "${YELLOW}[1/4]${NC} 生成 SSH 密钥对..."

SSH_DIR="$HOME/.ssh"
KEY_NAME="search_platform_$(date +%Y%m%d_%H%M%S)"
PRIVATE_KEY="$SSH_DIR/${KEY_NAME}"
PUBLIC_KEY="${PRIVATE_KEY}.pub"

if [ ! -d "$SSH_DIR" ]; then
    mkdir -p "$SSH_DIR"
    chmod 700 "$SSH_DIR"
fi

echo "  密钥路径: $PRIVATE_KEY"

# 生成密钥
ssh-keygen -t rsa -b 4096 -f "$PRIVATE_KEY" -N "" -C "search-platform@$(hostname)"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 密钥生成失败${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 密钥对生成成功${NC}"
echo ""

# ==================== 步骤 2: 复制公钥到远程服务器 ====================
echo -e "${YELLOW}[2/4]${NC} 复制公钥到远程服务器..."

echo "  正在复制公钥..."
ssh-copy-id -i "$PUBLIC_KEY" "$REMOTE_HOST"

if [ $? -ne 0 ]; then
    echo ""
    echo -e "${YELLOW}ssh-copy-id 失败，尝试手动复制...${NC}"
    echo ""
    echo "请手动执行以下步骤："
    echo ""
    echo "1. 查看公钥内容:"
    cat "$PUBLIC_KEY"
    echo ""
    echo "2. 登录远程服务器:"
    echo "   ssh $REMOTE_HOST"
    echo ""
    echo "3. 添加公钥到 authorized_keys:"
    echo "   mkdir -p ~/.ssh"
    echo "   chmod 700 ~/.ssh"
    echo "   echo '$(cat $PUBLIC_KEY)' >> ~/.ssh/authorized_keys"
    echo "   chmod 600 ~/.ssh/authorized_keys"
    echo ""
    exit 1
fi

echo -e "${GREEN}✓ 公钥已复制到远程服务器${NC}"
echo ""

# ==================== 步骤 3: 测试密钥登录 ====================
echo -e "${YELLOW}[3/4]${NC} 测试密钥登录..."

# 创建 SSH 配置
CONFIG_FILE="$SSH_DIR/config"
BACKUP_FILE="$SSH_DIR/config.backup.$(date +%Y%m%d_%H%M%S)"

if [ -f "$CONFIG_FILE" ]; then
    cp "$CONFIG_FILE" "$BACKUP_FILE"
    echo "  已备份原配置: $BACKUP_FILE"
fi

# 添加配置
echo "" >> "$CONFIG_FILE"
echo "# Search Platform - $REMOTE_HOST" >> "$CONFIG_FILE"
echo "Host $REMOTE_IP" >> "$CONFIG_FILE"
echo "    HostName $REMOTE_IP" >> "$CONFIG_FILE"
echo "    User $REMOTE_USER" >> "$CONFIG_FILE"
echo "    IdentityFile $PRIVATE_KEY" >> "$CONFIG_FILE"
echo "    IdentitiesOnly yes" >> "$CONFIG_FILE"
echo "    ServerAliveInterval 60" >> "$CONFIG_FILE"
echo "    ServerAliveCountMax 3" >> "$CONFIG_FILE"

echo "  已更新 SSH 配置: $CONFIG_FILE"

# 测试登录
echo "  测试 SSH 连接..."
if ssh -o StrictHostKeyChecking=no -o PasswordAuthentication=no "$REMOTE_HOST" "echo '密钥登录成功!'" 2>/dev/null; then
    echo -e "${GREEN}✓ 密钥登录测试成功${NC}"
else
    echo -e "${RED}❌ 密钥登录测试失败${NC}"
    exit 1
fi
echo ""

# ==================== 步骤 4: 保存配置信息 ====================
echo -e "${YELLOW}[4/4]${NC} 保存配置信息..."

CONFIG_FILE_SAVE="deployments/ssh-config.env"
cat > "$CONFIG_FILE_SAVE" << EOF
# SSH 密钥认证配置
# 生成时间: $(date)

REMOTE_HOST=$REMOTE_HOST
REMOTE_USER=$REMOTE_USER
REMOTE_IP=$REMOTE_IP
SSH_PRIVATE_KEY=$PRIVATE_KEY
SSH_PUBLIC_KEY=$PUBLIC_KEY
EOF

echo "  配置已保存: $CONFIG_FILE_SAVE"
echo ""

# ==================== 完成 ====================
echo "========================================"
echo -e "${GREEN}🎉 SSH 密钥认证配置完成！${NC}"
echo "========================================"
echo ""
echo "密钥信息:"
echo "  私钥: $PRIVATE_KEY"
echo "  公钥: $PUBLIC_KEY"
echo ""
echo "测试登录:"
echo "  ssh $REMOTE_HOST"
echo ""
echo "现在可以使用部署脚本了:"
echo "  cd deployments"
echo "  bash deploy-one-click.sh $REMOTE_HOST"
echo ""
