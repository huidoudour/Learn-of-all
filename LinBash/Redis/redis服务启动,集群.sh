
sudo mkdir -p /etc/redis
sudo cp /usr/share/doc/redis-*/redis.conf /etc/redis/redis_8001.conf
sudo cp /usr/share/doc/redis-*/redis.conf /etc/redis/redis_8002.conf
sudo cp /usr/share/doc/redis-*/redis.conf /etc/redis/redis_8003.conf
sudo cp /usr/share/doc/redis-*/redis.conf /etc/redis/redis_8004.conf
sudo cp /usr/share/doc/redis-*/redis.conf /etc/redis/redis_8005.conf
sudo cp /usr/share/doc/redis-*/redis.conf /etc/redis/redis_8006.conf



redis-server /etc/redis/redis_8001.conf
redis-server /etc/redis/redis_8002.conf
redis-server /etc/redis/redis_8003.conf
redis-server /etc/redis/redis_8004.conf
redis-server /etc/redis/redis_8005.conf
redis-server /etc/redis/redis_8006.conf


redis-cli --cluster create 192.168.128.132:8001 192.168.128.132:8002 192.168.128.132:8003 192.168.128.132:8004 192.168.128.132:8005 192.168.128.132:8006 --cluster-replicas 1




redis-cli --cluster create 127.0.0.1:8001 127.0.0.1:8002 127.0.0.1:8003 127.0.0.1:8004 127.0.0.1:8005 127.0.0.1:8006 --cluster-replicas 1




