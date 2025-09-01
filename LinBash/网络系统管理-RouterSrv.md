# 网络系统管理-RouterSrv

## 3、RouterSrv配置路由转发和IPTABLES

### SNAT

```bash
iptables -t nat -A POSTING -s 192.168.100.0/24 -o ens37 -j MASQUERADE
iptables -t nat -A POSTING -s 192.168.0.0/24 -o ens37 -j MASQUERADE
```

### DNAT

```bash
iptables -t nat -A PREOUTING -p udp -s 81.6.63.0/24 -d 81.6.63.254 --dport 53 -j DNAT --to 192.168.100.100
iptables -t nat -A PREOUTING -p tcp -s 81.6.63.0/24 -d 81.6.63.254 -m multiport --dport 53,80,443,465,993 -j DNAT --to 192.168.100.100
iptables -t nat -A PREOUTING -p tcp -s 81.6.63.0/24 -d 81.6.63.254 -m multiport --dport 20,21,137,138,139,444,445,3358 -j DNAT --to 192.168.100.200
```

