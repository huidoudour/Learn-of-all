# Redis服务启动,集群

> 适用于旧版方法，手动编辑配置文件

### 创建&复制配置文件

```bash
sudo mkdir -p /etc/redis
sudo cp /usr/share/doc/redis-*/redis.conf /etc/redis/redis_8001.conf
sudo cp /usr/share/doc/redis-*/redis.conf /etc/redis/redis_8002.conf
sudo cp /usr/share/doc/redis-*/redis.conf /etc/redis/redis_8003.conf
sudo cp /usr/share/doc/redis-*/redis.conf /etc/redis/redis_8004.conf
sudo cp /usr/share/doc/redis-*/redis.conf /etc/redis/redis_8005.conf
sudo cp /usr/share/doc/redis-*/redis.conf /etc/redis/redis_8006.conf

```

### 服务启动

```bash
redis-server /etc/redis/redis_8001.conf
redis-server /etc/redis/redis_8002.conf
redis-server /etc/redis/redis_8003.conf
redis-server /etc/redis/redis_8004.conf
redis-server /etc/redis/redis_8005.conf
redis-server /etc/redis/redis_8006.conf
```

### 集群创建

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

