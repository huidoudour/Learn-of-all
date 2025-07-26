
redis-cli --cluster create 192.168.128.132:8001 192.168.128.132:8002 192.168.128.132:8003 192.168.128.132:8004 192.168.128.132:8005 192.168.128.132:8006 --cluster-replicas 1



cp -p /etc/redis.conf /etc/redis/redis_8001.conf
cp -p /etc/redis.conf /etc/redis/redis_8002.conf
cp -p /etc/redis.conf /etc/redis/redis_8003.conf
cp -p /etc/redis.conf /etc/redis/redis_8004.conf
cp -p /etc/redis.conf /etc/redis/redis_8005.conf
cp -p /etc/redis.conf /etc/redis/redis_8006.conf




VIM

:%s/^#.*$//

:g/^$/d



redis-cli --cluster create 10.1.1.13:8001 10.1.1.13:8002 10.1.1.13:8003 10.1.1.14:8001 10.1.1.14:8002 10.1.1.14:8003 --cluster-replicas 1
