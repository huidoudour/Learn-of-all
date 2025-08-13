# 你好哇

```bash
nmcli connection show

cat /etc/os-release  					#UOS/CentOS
cat /etc/redhat-release					#CentOS
vim /etc/motd							#UOS
hostnamectl set-sethostname <word>
```

vim /etc/profile.d/login.sh

```bash
#用在UOS
#!/bin/bash
time=$(date)
name=$(hostname)
echo "****************************************"
echo "   ChinaSkills 2025 - CSK"
echo "       Module C Linux"
echo " "
echo "    >>$name<<"
echo ">>'OS-Version'<<"
echo ">>$time<<"
echo "****************************************"
```

```bash
#用在CentOS
#!/bin/bash
time=$(date)
name=$(hostname)
echo "****************************************"
echo "        ECS-Server by Aliyun"
echo "          Module C Linux"
echo " "
echo "            >>$name<<"
echo ">>CentOS Linux release 7.9.2009 (Core)<<"
echo ">>$time<<"
echo "****************************************"
```

```bash
#UOS
apt-get -y install isc-dhcp-server
vim /etc/default/isc-dhcp-server
>>
INTERFACESv4="ens33"
<

vim /etc/dhcp/dhcpd.conf
>>
subnet 10.5.5.0 netmask 255.255.255.0 {
	range 10.5.5.26 10.5.5.30;								#DHCP地址范围
	option domain-name-serveers nsl.internal.example.org;	#DNS服务器地址
	option domain-name "internal.example.org";				#域名
	option routers 10.5.5.1;								#网关
	option broaadcast-address 10.5.5.31;					#广播地址
	default-lease-time 600;									#默认释放时间
	max-least-time 7200;									#最大释放时间
}
<
```

