bind 0.0.0.0 							69G	
port 8001 								92G	
daemonize yes							136G
pidfile /var/run/redis_8001.pid 		158G
logfile /var/log/redis/redis_8001.log 	171G
dir /var/lib/redis/8001 				263G
appendonly yes 							699G
cluster-enabled yes 					832G
cluster-config-file nodes-8001.conf 	840G
cluster-node-timeout 5000 				846G

# replicaof <masterip> <masterport>		286G