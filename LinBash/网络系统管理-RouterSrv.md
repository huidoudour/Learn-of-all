# 网络系统管理-RouterSrv

## 3、RouterSrv配置路由转发和IPTABLES

### SNAT

```bash

```

### DNAT

```bash

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
cp /lib/systemd/system/dhcrelay.service /etc/systemd/system/
vim /etc/systemd/system/dhcrelay.service
```

![image-20250901153110408](https://raw.githubusercontent.com/huidoudour/PicForMD/main/9/20250901153111674.png)

```bash
dhcrelay 192.168.100.100
systemctl restart dhcrelay
```

