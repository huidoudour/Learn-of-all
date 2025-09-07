# Debian Linux 网络系统管理配置指南

## 第一部分：路由和防火墙配置

### 1. 开启路由转发功能

编辑 `/etc/sysctl.conf` 文件：
```bash
sudo nano /etc/sysctl.conf
```

添加或修改以下行：
```
net.ipv4.ip_forward=1
```

应用更改：
```bash
sudo sysctl -p
```

### 2. Iptables 配置

```bash
sudo iptables -F
sudo iptables -t nat -F

# 设置默认策略
sudo iptables -P INPUT DROP
sudo iptables -P FORWARD DROP
sudo iptables -P OUTPUT ACCEPT

# 允许本地回环
sudo iptables -A INPUT -i lo -j ACCEPT
sudo iptables -A OUTPUT -o lo -j ACCEPT

# 允许已建立的连接
sudo iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
sudo iptables -A FORWARD -m state --state ESTABLISHED,RELATED -j ACCEPT

# 始终允许来自指定网络的流量
sudo iptables -A INPUT -s 192.168.1.0/24 -j ACCEPT
sudo iptables -A INPUT -s 172.16.1.0/24 -j ACCEPT
sudo iptables -A INPUT -s 192.168.2.0/24 -j ACCEPT

sudo iptables -A FORWARD -s 192.168.1.0/24 -j ACCEPT
sudo iptables -A FORWARD -s 172.16.1.0/24 -j ACCEPT
sudo iptables -A FORWARD -s 192.168.2.0/24 -j ACCEPT

# 配置PAT(源地址转换)使内网能访问互联网
sudo iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE

# 假设eth0是外网接口，eth1是内网接口
# 允许内网访问外网
sudo iptables -A FORWARD -i eth1 -o eth0 -j ACCEPT

# 配置DNAT将外部访问转发到内部服务
# 例如，将外部访问80端口转发到内部192.168.1.100的80端口
sudo iptables -t nat -A PREROUTING -i eth0 -p tcp --dport 80 -j DNAT --to-destination 192.168.1.100:80
sudo iptables -A FORWARD -i eth0 -d 192.168.1.100 -p tcp --dport 80 -j ACCEPT

# 保存iptables规则
sudo apt-get install iptables-persistent
sudo netfilter-persistent save
```

## 第二部分：OpenVPN 服务器配置

### 1. 安装OpenVPN和Easy-RSA

```bash
sudo apt-get update
sudo apt-get install openvpn easy-rsa
```

### 2. 设置PKI (Public Key Infrastructure)

```bash
make-cadir ~/openvpn-ca
cd ~/openvpn-ca
```

编辑 `vars` 文件：
```bash
nano vars
```

修改以下内容（根据你的信息调整）：
```
export KEY_COUNTRY="CN"
export KEY_PROVINCE="Beijing"
export KEY_CITY="Beijing"
export KEY_ORG="WSC2025"
export KEY_EMAIL="admin@wsc2025.cn"
export KEY_OU="MyOrganizationalUnit"
export KEY_NAME="server"
```

初始化并构建CA：
```bash
source vars
./clean-all
./build-ca
```

### 3. 生成服务器证书和密钥

```bash
./build-key-server server
```

### 4. 生成Diffie-Hellman参数

```bash
./build-dh
```

### 5. 生成HMAC签名

```bash
openvpn --genkey --secret keys/ta.key
```

### 6. 生成客户端证书

```bash
source vars
./build-key client1
```

### 7. 配置OpenVPN服务器

复制示例配置文件：
```bash
gunzip -c /usr/share/doc/openvpn/examples/sample-config-files/server.conf.gz | sudo tee /etc/openvpn/server.conf
```

编辑配置文件：
```bash
sudo nano /etc/openvpn/server.conf
```

主要修改以下内容：
```
port 1194
proto udp
dev tun
ca /home/youruser/openvpn-ca/keys/ca.crt
cert /home/youruser/openvpn-ca/keys/server.crt
key /home/youruser/openvpn-ca/keys/server.key
dh /home/youruser/openvpn-ca/keys/dh2048.pem
server 192.168.2.0 255.255.255.0
push "route 192.168.1.0 255.255.255.0"
push "route 172.16.1.0 255.255.255.0"
ifconfig-pool-persist ipp.txt
push "dhcp-option DNS 8.8.8.8"
push "dhcp-option DNS 8.8.4.4"
keepalive 10 120
tls-auth /home/youruser/openvpn-ca/keys/ta.key 0
cipher AES-256-CBC
user nobody
group nogroup
persist-key
persist-tun
status openvpn-status.log
verb 3
explicit-exit-notify 1
```

### 8. 启用IP转发和配置防火墙

确保已按照第一部分启用了IP转发。

添加iptables规则允许VPN流量：
```bash
sudo iptables -A INPUT -p udp --dport 1194 -j ACCEPT
sudo iptables -A FORWARD -i tun0 -j ACCEPT
sudo iptables -A FORWARD -i tun0 -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT
sudo iptables -A FORWARD -i eth0 -o tun0 -m state --state RELATED,ESTABLISHED -j ACCEPT
sudo iptables -t nat -A POSTROUTING -s 192.168.2.0/24 -o eth0 -j MASQUERADE
```

### 9. 启动OpenVPN服务

```bash
sudo systemctl start openvpn@server
sudo systemctl enable openvpn@server
```

### 10. 创建客户端配置文件

创建客户端配置文件目录：
```bash
mkdir -p ~/client-configs/files
```

创建基础配置文件：
```bash
nano ~/client-configs/base.conf
```

内容如下：
```
client
dev tun
proto udp
remote router.wsc2025.cn 1194
resolv-retry infinite
nobind
persist-key
persist-tun
remote-cert-tls server
cipher AES-256-CBC
verb 3
```

创建生成客户端配置的脚本：
```bash
nano ~/client-configs/make_config.sh
```

内容如下：
```bash
#!/bin/bash

# First argument: Client identifier

KEY_DIR=~/openvpn-ca/keys
OUTPUT_DIR=~/client-configs/files
BASE_CONFIG=~/client-configs/base.conf

cat ${BASE_CONFIG} \
    <(echo -e '<ca>') \
    ${KEY_DIR}/ca.crt \
    <(echo -e '</ca>\n<cert>') \
    ${KEY_DIR}/${1}.crt \
    <(echo -e '</cert>\n<key>') \
    ${KEY_DIR}/${1}.key \
    <(echo -e '</key>\n<tls-auth>') \
    ${KEY_DIR}/ta.key \
    <(echo -e '</tls-auth>') \
    > ${OUTPUT_DIR}/${1}.ovpn
```

使脚本可执行：
```bash
chmod 700 ~/client-configs/make_config.sh
```

生成客户端配置文件：
```bash
cd ~/client-configs
./make_config.sh client1
```

生成的客户端配置文件在 `~/client-configs/files/client1.ovpn`，可以将其提供给客户端使用。

## 总结

以上配置完成了：
1. 路由转发功能开启
2. Iptables配置实现了：
   - wsc2025.cn域通过PAT访问互联网
   - 客户端通过DNAT访问对外服务
   - 严格的默认DROP策略
   - 允许指定网络的流量
3. OpenVPN服务器安装配置：
   - 使用192.168.2.0/24地址池
   - 客户端证书认证
   - 允许VPN客户端访问内部网络

记得根据你的实际网络接口和IP地址调整上述配置中的eth0/eth1等接口名称。