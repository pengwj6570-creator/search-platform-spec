# SSH 密钥认证配置指南

## 📋 概述

使用 SSH 密钥认证可以更安全、更便捷地登录远程服务器，无需每次输入密码。

---

## 🔧 方式一：自动配置（推荐）

### 步骤 1: 运行密钥配置脚本

```bash
cd D:/dev/claudecode/search-platform-spec/deployments
bash setup-ssh-key.sh 用户名@服务器IP
```

**示例：**
```bash
bash setup-ssh-key.sh root@192.168.1.100
```

### 步骤 2: 脚本会自动完成

1. **生成 SSH 密钥对**
   - 私钥: `~/.ssh/search_platform_YYYYMMDD_HHMMSS`
   - 公钥: `~/.ssh/search_platform_YYYYMMDD_HHMMSS.pub`

2. **复制公钥到远程服务器**
   - 自动使用 `ssh-copy-id` 命令
   - 首次需要输入一次远程服务器密码

3. **配置 SSH 客户端**
   - 更新 `~/.ssh/config` 文件
   - 添加密钥路径配置

4. **测试密钥登录**
   - 自动验证密钥认证是否工作

### 步骤 3: 验证配置

```bash
# 应该能直接登录，无需密码
ssh 用户名@服务器IP
```

---

## 🔨 方式二：手动配置

### 步骤 1: 生成本地密钥对

```bash
# Windows Git Bash
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"

# 按提示操作：
# - 保存路径: 默认 ~/.ssh/id_rsa (或自定义名称)
# - 密码短语: 直接回车（不设置密码）
```

### 步骤 2: 查看公钥

```bash
cat ~/.ssh/id_rsa.pub
```

输出示例：
```
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQC... your_email@example.com
```

### 步骤 3: 复制公钥到远程服务器

**方法 A: 使用 ssh-copy-id**
```bash
ssh-copy-id 用户名@服务器IP
# 首次需要输入密码
```

**方法 B: 手动复制**
```bash
# 1. 登录远程服务器
ssh 用户名@服务器IP

# 2. 在远程服务器上执行
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# 3. 将公钥内容粘贴到 authorized_keys
echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQC... your_email@example.com" >> ~/.ssh/authorized_keys

# 4. 设置正确权限
chmod 600 ~/.ssh/authorized_keys

# 5. 退出登录
exit
```

### 步骤 4: 配置 SSH 客户端

编辑本地 SSH 配置文件：

```bash
# Windows 路径
notepad ~/.ssh/config

# 或使用 vim
vim ~/.ssh/config
```

添加以下内容：

```
Host search-platform
    HostName 192.168.1.100
    User root
    IdentityFile ~/.ssh/id_rsa
    IdentitiesOnly yes
    ServerAliveInterval 60
    ServerAliveCountMax 3
```

### 步骤 5: 测试连接

```bash
# 使用配置的别名
ssh search-platform

# 或直接使用 IP
ssh root@192.168.1.100
```

---

## 🌐 方式三：使用 PuTTY (Windows 原生)

### 步骤 1: 使用 PuTTYgen 生成密钥

1. 下载并安装 [PuTTY](https://www.chiark.greenend.org.uk/~sgtatham/putty/)
2. 打开 **PuTTYgen**
3. 点击 **Generate** 生成密钥对
4. 保存私钥：点击 **Save private key** (保存为 `.ppk` 文件)
5. 复制公钥：复制框中的公钥内容

### 步骤 2: 在远程服务器添加公钥

```bash
# 登录服务器
ssh 用户名@服务器IP

# 编辑 authorized_keys
vim ~/.ssh/authorized_keys

# 粘贴公钥内容（一行）
ssh-rsa AAAAB3NzaC1yc2E... [...]

# 保存并设置权限
chmod 600 ~/.ssh/authorized_keys
```

### 步骤 3: 配置 PuTTY

1. 打开 **PuTTY**
2. **Session** → Host Name: `服务器IP`
3. **Connection** → **Data** → Auto-login username: `用户名`
4. **Connection** → **SSH** → **Auth** → Credentials:
   - Private key file: 选择保存的 `.ppk` 文件
5. 回到 **Session**，保存配置:
   - Host Name: `search-platform`
   - 点击 **Save**

---

## 📝 密钥管理最佳实践

### 1. 为不同项目使用不同密钥

```bash
# 为搜索平台项目单独生成密钥
ssh-keygen -t rsa -b 4096 -f ~/.ssh/search_platform_key
```

### 2. 在 SSH config 中配置多台服务器

```
# ~/.ssh/config

# 搜索平台 - 测试环境
Host search-test
    HostName 192.168.1.100
    User root
    IdentityFile ~/.ssh/search_platform_key

# 搜索平台 - 生产环境
Host search-prod
    HostName 192.168.1.200
    User ubuntu
    IdentityFile ~/.ssh/search_platform_prod_key

# GitHub
Host github.com
    HostName github.com
    User git
    IdentityFile ~/.ssh/github_key
```

### 3. 保护私钥安全

| 操作 | 说明 |
|------|------|
| 私钥权限 | 必须是 `600` (仅所有者可读写) |
| .ssh 目录权限 | 必须是 `700` (仅所有者可访问) |
| 永不分享私钥 | 只分享公钥 (`*.pub` 文件) |
| 定期轮换 | 建议每 6-12 个月更换密钥 |

---

## 🔍 故障排查

### 问题 1: 提示 "Permission denied (publickey)"

**原因：** 服务器未正确配置公钥或密钥路径错误

**解决：**
```bash
# 1. 检查本地密钥
ls -la ~/.ssh/

# 2. 使用调试模式查看详细日志
ssh -v 用户名@服务器IP

# 3. 确认服务器上的公钥
ssh 用户名@服务器IP "cat ~/.ssh/authorized_keys"
```

### 问题 2: 提示 "WARNING: UNPROTECTED PRIVATE KEY FILE!"

**原因：** 私钥文件权限过于开放

**解决：**
```bash
chmod 600 ~/.ssh/your_private_key
chmod 700 ~/.ssh
```

### 问题 3: Windows Git Bash 找不到密钥

**原因：** Git Bash 使用不同的 HOME 目录

**解决：**
```bash
# 确保 Git Bash 使用的 SSH 目录正确
echo $HOME

# 通常在 C:/Users/你的用户名/.ssh
ls ~/.ssh/

# 如果密钥在其他位置，创建符号链接
ln -s /c/path/to/key ~/.ssh/id_rsa
```

---

## 🚀 配置完成后，开始部署

```bash
cd D:/dev/claudecode/search-platform-spec/deployments

# 现在可以直接部署，无需输入密码
bash deploy-one-click.sh 用户名@服务器IP
```

---

## 📚 相关文件

| 文件 | 说明 |
|------|------|
| `deployments/setup-ssh-key.sh` | 自动配置 SSH 密钥脚本 |
| `deployments/ssh-config.env` | 保存的 SSH 配置信息 |
| `~/.ssh/config` | SSH 客户端配置文件 |
| `~/.ssh/authorized_keys` | 服务器端公钥列表 |
