#验证
[root@ecs2 ~]# redis-cli -c -h 192.168.128.132 -p 8001
192.168.128.132:8001>
192.168.128.132:8001>
192.168.128.132:8001>
192.168.128.132:8001>
192.168.128.132:8001>
192.168.128.132:8001> exit
[root@ecs2 ~]#
[root@ecs2 ~]#
[root@ecs2 ~]#
[root@ecs2 ~]# redis-cli -h 192.168.128.132 -p 8001 cluster info
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:6
cluster_size:3
cluster_current_epoch:6
cluster_my_epoch:1
cluster_stats_messages_ping_sent:1119
cluster_stats_messages_pong_sent:1097
cluster_stats_messages_sent:2216
cluster_stats_messages_ping_received:1092
cluster_stats_messages_pong_received:1119
cluster_stats_messages_meet_received:5
cluster_stats_messages_received:2216
[root@ecs2 ~]# redis-cli -h 192.168.128.132 -p 8002 cluster info
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:6
cluster_size:3
cluster_current_epoch:6
cluster_my_epoch:2
cluster_stats_messages_ping_sent:1143
cluster_stats_messages_pong_sent:1116
cluster_stats_messages_meet_sent:4
cluster_stats_messages_sent:2263
cluster_stats_messages_ping_received:1113
cluster_stats_messages_pong_received:1147
cluster_stats_messages_meet_received:3
cluster_stats_messages_received:2263
[root@ecs2 ~]# redis-cli -h 192.168.128.132 -p 8003 cluster info
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:6
cluster_size:3
cluster_current_epoch:6
cluster_my_epoch:3
cluster_stats_messages_ping_sent:1128
cluster_stats_messages_pong_sent:1107
cluster_stats_messages_meet_sent:4
cluster_stats_messages_sent:2239
cluster_stats_messages_ping_received:1105
cluster_stats_messages_pong_received:1132
cluster_stats_messages_meet_received:2
cluster_stats_messages_received:2239
[root@ecs2 ~]# redis-cli -h 192.168.128.132 -p 8004 cluster info
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:6
cluster_size:3
cluster_current_epoch:6
cluster_my_epoch:2
cluster_stats_messages_ping_sent:1108
cluster_stats_messages_pong_sent:1149
cluster_stats_messages_meet_sent:4
cluster_stats_messages_sent:2261
cluster_stats_messages_ping_received:1148
cluster_stats_messages_pong_received:1112
cluster_stats_messages_meet_received:1
cluster_stats_messages_received:2261
[root@ecs2 ~]# redis-cli -h 192.168.128.132 -p 8005 cluster info
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:6
cluster_size:3
cluster_current_epoch:6
cluster_my_epoch:3
cluster_stats_messages_ping_sent:1124
cluster_stats_messages_pong_sent:1148
cluster_stats_messages_meet_sent:4
cluster_stats_messages_sent:2276
cluster_stats_messages_ping_received:1146
cluster_stats_messages_pong_received:1128
cluster_stats_messages_meet_received:2
cluster_stats_messages_received:2276
[root@ecs2 ~]# redis-cli -h 192.168.128.132 -p 8006 cluster info
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:6
cluster_size:3
cluster_current_epoch:6
cluster_my_epoch:1
cluster_stats_messages_ping_sent:1143
cluster_stats_messages_pong_sent:1163
cluster_stats_messages_meet_sent:1
cluster_stats_messages_sent:2307
cluster_stats_messages_ping_received:1159
cluster_stats_messages_pong_received:1144
cluster_stats_messages_meet_received:4
cluster_stats_messages_received:2307
[root@ecs2 ~]#