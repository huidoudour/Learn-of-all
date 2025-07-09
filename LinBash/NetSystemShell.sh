systemctl disable --now firewalld #停用并关闭防火墙
firewall-cmd --zone=public --add-port=XX/tcp --add-port=XX/udp #启用对应的端口号
firewall-cmd --permanent --add-service=XX #放行对应服务
systemctl restart firewalld #重启防火墙服务使配置生效
firewall-cmd --list-all #查询验证配置结果

#IspSrv-DHCP
apt -y install isc-dhcp-server 
vim /etc/default/isc-dhcp-server
	INTERFACESv4="ens33" #修改为对应网卡设备名
vim /etc/dhcp/dhcpd.conf #编辑主配置文件
/*
	subnet 81.6.63.0 netmask 255.255.255.0 { #定义子网
		range 81.6.63.110 81.6.63.190; #DHCP IP 范围
		option domain-name-servers 81.6.63.100; #DNS服务器
		option domain-name "chinaskills.cn"; #域名
		option routers 81.6.63.254; #默认网关
		default-lease-time 600; #默认租期时间
		max-lease-time 7200; #最大租期时间
	}
	host outsidecli {
		hardware ethernet 00:0C:29:2C:E0:4D;
		fixed-address 81.6.63.190;
	}
*/
systemctl enable --now isc-dhcp-server
#OutsideCli
nmcli connection up ens33
#DHCP end
#IspSrv-DNS
apt -y install bind9
cd /etc/bind
vim /etc/bind/named.conf.local
/*
	//
	// Do any local configuration here
	//

	// Consider adding the 1918 zones here, if they are not used in your
	// organization
	//include "/etc/bind/zones.rfc1918";
	zone "." IN {
	        type master;
	        file "/etc/bind/db.root";
	};
	zone "chinaskills.cn" IN {
	        type slave;
	        masters { 81.6.63.254; };
	};
*/
cp -a /etc/bind/db.local /etc/bind/db.root
vim /etc/bind/db.root
/*
	;
	; BIND data file for local loopback interface
	;
	$TTL    604800
	@       IN      SOA     localhost. root.localhost. (
	                              2         ; Serial
	                         604800         ; Refresh
	                          86400         ; Retry
	                        2419200         ; Expire
	                         604800 )       ; Negative Cache TTL
	;
	@       IN      NS      localhost.
	*       IN      A       81.6.63.100
*/
systemctl enable --now bind9
#IspSrv-Web(Nginx+PHP/FastCGI)
apt -y install nginx php7.3-fpm php7.3 php7.3-cli #如无软件包需要联网下载

echo " <?php echo "Welcome to 2025 Computer Network Application contest"; ?>" > /mut/crypt/index.php








