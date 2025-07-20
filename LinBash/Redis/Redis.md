# 在Kylin/OpenEuler上搭建Redis Cluster集群

下面是在Kylin或OpenEuler操作系统上搭建Redis Cluster集群的步骤，使用端口8001-8003作为主节点，8004-8006作为从节点。

## 1. 安装Redis

首先在ecs1上安装Redis：

```bash
# 更新系统
sudo yum update -y

# 安装依赖
sudo yum install -y gcc make tcl

# 下载并解压Redis
wget http://download.redis.io/releases/redis-6.2.6.tar.gz
tar xzf redis-6.2.6.tar.gz
cd redis-6.2.6

# 编译安装
make
sudo make install

#安装成功后不会自动复制配置文件，可以手动复制redis文件夹内的redis.conf文件到/etc/
```

## 2. 创建Redis集群配置文件

创建6个Redis实例的配置文件和目录结构：

```bash
# 创建集群目录结构
for port in {8001..8006}; do
  sudo mkdir -p /etc/redis/${port}
  sudo mkdir -p /var/log/redis/${port}
  sudo mkdir -p /var/lib/redis/${port}
done
```

为每个端口创建配置文件：

```bash
for port in {8001..8006}; do
  cat <<EOF | sudo tee /etc/redis/${port}/redis.conf
port ${port}
bind 0.0.0.0
cluster-enabled yes
cluster-config-file /var/lib/redis/${port}/nodes.conf
cluster-node-timeout 5000
appendonly yes
daemonize yes
pidfile /var/run/redis_${port}.pid
logfile /var/log/redis/${port}/redis.log
dir /var/lib/redis/${port}
EOF
done
```

## 3. 启动Redis实例

启动所有Redis实例：

```bash
for port in {8001..8006}; do
  redis-server /etc/redis/${port}/redis.conf
done
```

验证Redis实例是否正常运行：

```bash
ps -ef | grep redis
```

## 4. 创建Redis集群

使用redis-cli创建集群（3主3从）：

```bash
redis-cli --cluster create \
  127.0.0.1:8001 \
  127.0.0.1:8002 \
  127.0.0.1:8003 \
  127.0.0.1:8004 \
  127.0.0.1:8005 \
  127.0.0.1:8006 \
  --cluster-replicas 1
```

在提示时输入"yes"确认配置。

## 5. 配置防火墙

允许其他主机访问Redis集群端口：

```bash
# 开放Redis集群端口
for port in {8001..8006}; do
  sudo firewall-cmd --permanent --add-port=${port}/tcp
done

# 开放集群总线端口（通常为Redis端口+10000）,<可跳过>
for port in {18001..18006}; do
  sudo firewall-cmd --permanent --add-port=${port}/tcp
done

# 重新加载防火墙
sudo firewall-cmd --reload
```

## 6. 验证集群状态

```bash
# 查看集群节点信息
redis-cli -p 8001 cluster nodes

# 检查集群健康状态
redis-cli --cluster check 127.0.0.1:8001

#ping测试
redis-cli -h <服务器IP> -p <端口> ping
#批量
for i in {1..6};do redis-cli -p 800$i -h <服务器IP> ping;done
```

## 7. 从其他主机访问集群

其他主机可以通过以下方式访问Redis集群：

1. 安装Redis客户端：
   ```bash
   sudo yum install -y redis
   ```

2. 使用redis-cli连接集群：
   ```bash
   redis-cli -c -h <ecs1_IP> -p 8001
   ```

3. 或者在应用程序中使用Redis集群客户端库，配置所有主节点地址。

## 8. 持久化配置（可选）

如果需要更可靠的持久化，可以修改配置文件中的以下参数：

```conf
appendonly yes
appendfsync everysec
save 900 1
save 300 10
save 60 10000
```

------

# Redis多服务器集群创建问题解决

遇到集群节点长时间无法完成握手（`Waiting for the cluster to join` 持续显示......）的问题，即使防火墙和网络连通性正常，仍可能有以下原因和解决方案：

---

### **1. 常见原因排查**
#### 1.1 **集群总线端口未开放**
- Redis Cluster 需要**两个端口**：
  - **客户端访问端口**（如8001-8006）
  - **集群总线端口**（客户端端口 + 10000，如18001-18006）
  

**检查方法：**
```bash
# 在所有节点执行
netstat -tulnp | grep redis
# 应看到类似输出：
# tcp  0  0 0.0.0.0:8001  0.0.0.0:*  LISTEN  [redis-server]
# tcp  0  0 0.0.0.0:18001 0.0.0.0:*  LISTEN  [redis-server]
```

**解决方案：**
```bash
# 开放总线端口（所有节点执行）
firewall-cmd --permanent --add-port=18001-18006/tcp
firewall-cmd --reload
```

#### 1.2 **绑定地址配置错误**
- 如果 `redis.conf` 中 `bind` 设置为 `127.0.0.1`，其他节点无法访问

**检查方法：**
```bash
grep "^bind" /etc/redis/*.conf
# 正确应显示：`bind 0.0.0.0` 或服务器实际IP
```

**解决方案：**
修改所有节点的配置文件：
```bash
sed -i 's/^bind .*/bind 0.0.0.0/g' /etc/redis/*.conf
redis-cli -p 8001 shutdown
redis-server /etc/redis/8001.conf
```

#### 1.3 **节点间DNS解析问题**
- 如果使用主机名而非IP，需确保所有节点能互相解析

**测试方法：**
```bash
# 在server1上测试
ping 192.168.100.122
telnet 192.168.100.122 8002
telnet 192.168.100.122 18002
```

---

### **2. 强制手动修复步骤**
如果等待超时，可以手动完成集群组建：

#### 2.1 **在所有节点执行集群握手**
```bash
# 以 server1 为基准，其他节点加入集群
redis-cli -p 8001 cluster meet 192.168.100.122 8002
redis-cli -p 8001 cluster meet 192.168.100.123 8003
redis-cli -p 8001 cluster meet 192.168.100.124 8004
redis-cli -p 8001 cluster meet 192.168.100.125 8005
redis-cli -p 8001 cluster meet 192.168.100.126 8006
```

#### 2.2 **验证节点状态**
```bash
redis-cli -p 8001 cluster nodes
# 应看到所有节点状态为 `connected`
```

#### 2.3 **手动分配主从关系**
如果自动分配失败，需手动执行：
```bash
# 设置从节点（在从节点服务器上执行）
redis-cli -p 8004 cluster replicate aeb9e84bac8ec4b41334aabb1cb87359995e607d  # 8004 -> 8003
redis-cli -p 8005 cluster replicate a259a2db1f8a6a772bebefe61057608db841fab2  # 8005 -> 8001
redis-cli -p 8006 cluster replicate 4f37749bf44931e65e5137cb39f6970f14430034  # 8006 -> 8002
```

---

### **3. 关键配置检查项**
在所有节点的 `redis.conf` 中确认：
```ini
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 15000  # 可适当增大超时时间
bind 0.0.0.0
protected-mode no  # 如果未设置密码，需关闭保护模式
```

---

### **4. 日志分析**
查看具体错误原因：
```bash
# 在所有节点检查日志
tail -n 100 /var/log/redis/8001.log
# 重点关注以下错误：
# - Connection refused
# - Failed cluster meeting
# - Can't communicate with peer
```

---

### **5. 快速重建集群**
如果问题持续，彻底清理后重建：
```bash
# 所有节点执行：
redis-cli -p 8001 shutdown
rm -f /var/lib/redis/nodes*.conf
redis-server /etc/redis/8001.conf
```
然后重新执行集群创建命令。

---

### **6. 网络层深度检查**
如果仍失败，进行底层网络测试：
```bash
# 在server1上对其他节点执行：
for ip in 192.168.100.{122..126}; do
  echo "Testing $ip:"
  tcping $ip 8001
  tcping $ip 18001
done
```
（需先安装 `tcping`：`yum install tcping`）

---

通过以上步骤，90%的集群握手问题可以解决。如果仍卡住，可能是网络设备（如交换机ACL）限制了节点间通信，需联系网络管理员检查。