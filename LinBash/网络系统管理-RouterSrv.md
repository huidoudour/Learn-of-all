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

### 查询是否成功配置
```bash
iptables -t nat -nvL POSTROUTING
iptables -t nat -nvL PREROUTING
```

![image-20250901145245534](https://raw.githubusercontent.com/huidoudour/PicForMD/main/9/20250901145247595.png)



## 4、配置DHCP、DHCP relay（DHCP中继）和WEB

### 配置
```bash
mkdir -p /mupt/crypt
vim /mupt/crypt/index.php
>
Welcome to 2023 computer Application contest!
>

```

```bash
cd /etc/nginx/
cp sites-available/default /etc/nginx/conf.d/ispweb.conf
rm -rf sites-avilable/default
rm -rf sites-enabled/default

cd /etc/nginx/conf.d/
vim /etc/nginx/conf.d/ispweb.conf
```

### 安装服务

```bash
yum -y install dhcp
cp /lib/systemctl/system/dhcrelay.service /etc/systemctl/system/
vim /etc/systemctl/system/dhcrelay.service
```

![image-20250901153110408](https://raw.githubusercontent.com/huidoudour/PicForMD/main/9/20250901153111674.png)

```bash
dhcrelay 192.168.100.100
systemctl restart dhcrelay
```

