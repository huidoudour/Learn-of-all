当你对 Redis 配置文件进行了修改后，需要重新加载或重启 Redis 服务以使更改生效。以下是几种方法来重新加载或重启 Redis 服务。

### 方法一：使用 `systemctl` 重启 Redis 服务

如果你是通过 `systemctl` 管理 Redis 服务的，可以使用以下命令来重启特定的 Redis 实例。

1. **停止服务**：

   ```bash
   sudo systemctl stop redis_8001
   sudo systemctl stop redis_8002
   sudo systemctl stop redis_8003
   sudo systemctl stop redis_8004
   sudo systemctl stop redis_8005
   sudo systemctl stop redis_8006
   ```

2. **启动服务**：

   ```bash
   sudo systemctl start redis_8001
   sudo systemctl start redis_8002
   sudo systemctl start redis_8003
   sudo systemctl start redis_8004
   sudo systemctl start redis_8005
   sudo systemctl start redis_8006
   ```

### 方法二：使用 `redis-cli` 重新加载配置

如果你不想完全重启 Redis 服务，可以使用 `redis-cli` 命令来重新加载配置文件。这适用于某些配置项的更改，但不是所有的配置项都可以在运行时重新加载。

1. **连接到 Redis 服务**：

   ```bash
   redis-cli -p 8001
   ```

2. **重新加载配置**：

   ```bash
   CONFIG REWRITE
   ```

   或者，如果你只是想重新加载特定的配置项，可以使用 `CONFIG SET` 命令。例如：

   ```bash
   CONFIG SET maxmemory 100mb
   ```

### 方法三：手动重启 Redis 服务

如果你没有使用 `systemd` 管理 Redis 服务，可以手动停止和启动 Redis 服务。

1. **停止服务**：

   ```bash
   sudo pkill -f "redis-server *:8001"
   sudo pkill -f "redis-server *:8002"
   sudo pkill -f "redis-server *:8003"
   sudo pkill -f "redis-server *:8004"
   sudo pkill -f "redis-server *:8005"
   sudo pkill -f "redis-server *:8006"
   ```

2. **启动服务**：

   ```bash
   sudo redis-server /etc/redis/8001.conf
   sudo redis-server /etc/redis/8002.conf
   sudo redis-server /etc/redis/8003.conf
   sudo redis-server /etc/redis/8004.conf
   sudo redis-server /etc/redis/8005.conf
   sudo redis-server /etc/redis/8006.conf
   ```

### 验证服务状态

无论你选择哪种方法，都可以通过以下命令来验证 Redis 服务的状态：

```bash
sudo systemctl status redis_8001
sudo systemctl status redis_8002
sudo systemctl status redis_8003
sudo systemctl status redis_8004
sudo systemctl status redis_8005
sudo systemctl status redis_8006
```

或者使用 `redis-cli` 连接到某个实例并检查其状态：

```bash
redis-cli -p 8001 ping
```

如果返回 `PONG`，则说明该实例正在运行。

希望这些方法能帮助你成功重新加载或重启 Redis 服务。如果有任何问题，请随时提问！