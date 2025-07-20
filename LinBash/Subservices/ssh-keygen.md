# 配置主机间互相免密登录

> 源于OpenEuler

```bash
#安装软件包
yum -y install sshpass
#生成主机密钥
ssh-keygen

#复制当前主机公钥到其他主机-IP形式
for i in {1..3}; do sshpass -p 'Zjs0202520'  ssh-copy-id -o StrictHostKeyChecking=no root@192.168.128.12$i; done
#复制当前主机公钥到其他主机-域名形式
for i in {1..3}; do sshpass -p 'Zjs0202520' ssh-copy-id -o StrictHostKeyChecking=no ecs$i.xckt.com.; done

```

