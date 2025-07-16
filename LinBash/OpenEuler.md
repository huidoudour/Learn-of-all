```bash
systemctl stop firwwalld
setenforce 0
firewall-cmd --list-all
dnf -y install bind-chroot
systemctl start/restart/stop firewalld
firewall-cmd --zone=public --add-port=xx/tcp | --add-port=xx/udp	→详见最底部
grep -n "<字段>" <文件>
```

## 1、DNS

```bash
yum -y install bind-chroot
systemctl enable --now named

vim /etc/namd.conf
options {
        listen-on port 53 { any; };
			……
			……
        allow-query     { any; };
};

vim /etc/named.rfc1912.zones
zone "xckt.com" IN {
        type master;
        file "xckt.com.zone";
        allow-update { none; };
};

zone "239.168.192.in-addr.arpa" IN {
        type master;
        file "239.168.192.zone";
        allow-update { none; };
};

```

## 2、sshpass

```bash
ssh-keygen
yum -y install sshpass

for i in {9..9}; do sshpass -p 'Zjs0202520'  ssh-copy-id -o StrictHostKeyChecking=no root@192.168.239.12$i; done

for i in {0..1}; do sshpass -p 'Zjs0202520'  ssh-copy-id -o StrictHostKeyChecking=no root@192.168.239.13$i; done

for i in {1..3}; do sshpass -p 'Zjs0202520' ssh-copy-id -o StrictHostKeyChecking=no ecs$i.xckt.com.; done



ssh-copy-id root@192.168.128.* -o StrictHostKeyChecking=no
ssh-copy-id ecs*.xckt.com -o StrictHostKeyChecking=no
```

## 3、CA服务器

https://blog.csdn.net/m0_60984906/article/details/128996331?fromshare=blogdetail&sharetype=blogdetail&sharerId=128996331&sharerefer=PC&sharesource=Tyxing206&sharefrom=from_link

原题

```
原题：
配置 linux1 为 CA 服务器,为 linux 主机颁发证书。证书颁发机构有效期 10 年，公用名为linux1.skills.com。申请并颁发一张供 linux 服务器使用的证 书，证书信息：有效期=5 年，公用名=skills.com，国家=CN，省=Beijing，城市 =Beijing，组织=skills，组织单位=system，使用者可选名称=*.skills.com 和 skills.com。将证书 skills.crt 和私钥 skills.key 复制到需要证书的 linux 服务器/etc/ssl 目录。浏览器访问 https 网站时，不出现证书警告信息
```

## 解析：

先安装证书组件openssl

```bash
yum install openssl* -y
```

切换至CA的工作目录

```bash
cd /etc/pki/CA/
```

创建编号文件

```bash
touch index.txt
echo 00 > serial
```

然后创建自签证书私钥

```bash
openssl genrsa -out ca.key 2048
```

使用此私钥创建CA根证书的请求文件

```bash
openssl req -new -out ca.csr -key ca.key
 
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [XX]:CN                //国家
State or Province Name (full name) []:Beijing       //省
Locality Name (eg, city) [Default City]:Beijing     //城市
Organization Name (eg, company) [Default Company Ltd]:skills    //组织
Organizational Unit Name (eg, section) []:system    //组织单位
Common Name (eg, your name or your server's hostname) []:linux1.skills.com    //公用名
Email Address []:                                    //邮箱(回车跳过)
Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:                            //密码（回车跳过）
An optional company name []:                        //公司名(回车跳过)
```

颁发CA根证书

```bash
openssl x509 -req -days 3650 -in ca.csr -signkey ca.key -out ca.crt
```

自签名证书就申请完成了 然后编辑一下证书颁发规则的文件

```bash
vim /etc/pki/tls/openssl.cnf
 
167  req_extensions = v3_req # The extensions to add to a certificate request    //取消注释
213 basicConstraints=CA:TRUE        //更改为TRUE
235 [ v3_req ]
236 subjectAltName = @alt_names     //在[ v3_req ]字段中添加此行
240 basicConstraints = CA:TRUE      //更改为TRUE
242 [alt_names]                //添加此字段
243 DNS.1=*.skills.com         //使用者可选名称1
244 DNS.2=skills.com           //使用者可选名称2
```

然后申请一下服务器证书私钥

```bash
openssl genrsa -out skills.key 2048
```

使用私钥创建服务器证书请求文件

```bash
openssl req -new -key skills.key -out skills.csr -config /etc/pki/tls/openssl.cnf -extensions v3_req    //使用私钥创建服务器证书请求文件
 
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [XX]:CN            //国家
State or Province Name (full name) []:Beijing   //省
Locality Name (eg, city) [Default City]:Beijing //城市
Organization Name (eg, company) [Default Company Ltd]:skills    //组织
Organizational Unit Name (eg, section) []:system                //组织单位
Common Name (eg, your name or your server's hostname) []:skills.com    //公用名
Email Address []:                            //邮箱(回车跳过)
Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:                    //密码(回车跳过)
An optional company name []:                //公司名(回车跳过)
```

使用请求文件颁发证书

```bash
openssl ca -in skills.csr  -out skills.crt -cert ca.crt  -keyfile ca.key -extensions v3_req -days 1825 -config /etc/pki/tls/openssl.cnf     //颁发服务器证书
 
Using configuration from /etc/pki/tls/openssl.cnf
Check that the request matches the signature
Signature ok
Certificate Details:
        Serial Number: 0 (0x0)
        Validity
            Not Before: Feb 12 16:16:02 2023 GMT
            Not After : Feb 11 16:16:02 2028 GMT
        Subject:
            countryName               = CN
            stateOrProvinceName       = Beijing
            organizationName          = skills
            organizationalUnitName    = system
            commonName                = skills.com
        X509v3 extensions:
            X509v3 Subject Alternative Name: 
                DNS:*.skills.com, DNS:skills.com
            X509v3 Basic Constraints: 
                CA:TRUE
            X509v3 Key Usage: 
                Digital Signature, Non Repudiation, Key Encipherment
Certificate is to be certified until Feb 11 16:16:02 2028 GMT (1825 days)
Sign the certificate? [y/n]:y            //输入y
 
 
1 out of 1 certificate requests certified, commit? [y/n]y        //输入y
Write out database with 1 new entries
Data Base Updated
```



## 4、Apache服务器

```
vim /etc/selinux/config
SELINUX=enforcing
	↓
SELINUX=disabled

yum -y install httpd

systemctl start httpd
systemctl enable httpd

vim /var/www/html/
mkdir 6401 6402 6403
vim index.html #在上面目录下各创建一个，随便写点啥，做测试

vim /etc/httpd/conf/httpd.conf
#使用端口
<VirtualHost *:80 >
        DocumentRoot /var/www/html
        ServerName www.xckt.com
         <Directory /var/www/html>
         AllowOverride None
         Require all granted
         </Directory>
</VirtualHost>
```

![image-20220815102438289](https://cdn.jsdelivr.net/gh/limuhuaxia/picgo/img/202208151700054.png)

## 5、81

```bash
"/etc/httpd/conf.d/xckt.com.conf"
#主配置
<VirtualHost *:81 >
        DocumentRoot /var/www/html/error
        ServerName 192.168.239.129
        <Directory /var/www/html/error>
        Allowoverride None
        Require all denied
        </Directory>
</VirtualHost>
<VirtualHost *:81 >
        DocumentRoot /var/www/html/
        ServerName www.xckt.com
        <Directory /var/www/html/>
        Allowoverride None
        Require all granted
        </Directory>
</VirtualHost>



#重定向
<VirtualHost *:81>
        ServerName www.xckt.com
        DocumentRoot /var/www/html
</VirtualHost>
<VirtualHost *:81>
        ServerName xckt.com
        Redirect permanent / http://www.xckt.com:81/
</VirtualHost>
<VirtualHost *:81>
        ServerName ecs1.xckt.com
        ServerAlias *.xckt.com
        Redirect 301 / http://www.xckt.com:81/
</VirtualHost>
```

## 6、MPM的worker模式

```bash
配置MPM工作模式为worker
cd /etc/httpd/conf.modules.d/
修改 00-mpm.conf文件配置
注释>#LoadModule mpm_event_module modules/mod_mpm_event.so
取消注释>LoadModule mpm_worker_module modules/mod_mpm_worker.so
```

## 7、SSL

------

### 一、SSL配置

如果客户端访问 Apache 服务时必须有 SSL 证书，可以按照以下步骤进行配置：

**一、安装 SSL 证书**

1. 获取 SSL 证书：
   - 可以从受信任的证书颁发机构（CA）购买 SSL 证书，或者使用免费的证书颁发机构如 Let's Encrypt。
   - 根据证书颁发机构的指南，申请并获取 SSL 证书。通常，你会收到一个包含证书文件（如 `.crt` 或 `.pem` 文件）和私钥文件（如 `.key` 文件）的压缩包。

2. 安装证书和私钥：
   - 将证书文件和私钥文件复制到服务器上的适当位置。通常，你可以将它们放在 `/etc/ssl/certs`（证书文件）和 `/etc/ssl/private`（私钥文件）目录下，但具体位置可以根据你的服务器配置而有所不同。
   - 确保证书文件和私钥文件的权限设置正确，以防止未经授权的访问。通常，私钥文件应该只有服务器管理员可读，而证书文件可以被 Web 服务器进程读取。

**二、配置 Apache 以使用 SSL**

1. 打开 Apache 配置文件：
   - 通常，Apache 的主配置文件是 `/etc/httpd/conf/httpd.conf`，但具体位置可能因操作系统和安装方式而有所不同。
   - 使用文本编辑器打开配置文件，并确保你有足够的权限进行编辑。

2. 启用 SSL 模块：
   - 在配置文件中查找以下内容：
     ```
     LoadModule ssl_module modules/mod_ssl.so
     ```
   - 如果没有找到这个指令，说明 SSL 模块没有加载，你需要添加它以启用 SSL 支持。

3. 创建或编辑虚拟主机配置：
   - 在配置文件中，你可以找到或添加虚拟主机配置部分。虚拟主机配置允许你为不同的域名或 IP 地址设置不同的服务器配置。
   - 例如，以下是一个简单的虚拟主机配置示例，用于启用 SSL：
     ```
     <VirtualHost *:443>
         ServerName yourdomain.com
         DocumentRoot /var/www/yourdomain.com/public_html

         SSLEngine on
         SSLCertificateFile /etc/ssl/certs/yourdomain.crt
         SSLCertificateKeyFile /etc/ssl/private/yourdomain.key

     </VirtualHost>
     ```
   - 在这个示例中，`ServerName` 是你的域名，`DocumentRoot` 是你的网站文件的目录。`SSLEngine on` 启用 SSL，`SSLCertificateFile` 和 `SSLCertificateKeyFile` 分别指定了证书文件和私钥文件的位置。
   
4. 配置强制 SSL：
   - 为了确保客户端必须使用 SSL 连接，可以在虚拟主机配置中添加以下指令：
     ```
     RewriteEngine on
     RewriteCond %{HTTPS} off
     RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [R=301,L]
     ```
   - 这些指令使用 `mod_rewrite` 模块将所有的 HTTP 请求重定向到 HTTPS。`RewriteCond` 检查请求是否不是通过 HTTPS 进行的，如果是，则 `RewriteRule` 将请求重定向到 HTTPS 版本的 URL。

**三、测试 SSL 配置**

1. 保存并关闭配置文件：
   
- 在编辑完成后，确保保存并关闭配置文件。
  
2. 重新启动 Apache 服务器：
   - 在 Linux 系统中，可以使用以下命令重新启动 Apache 服务器：
     ```
     sudo systemctl restart httpd
     ```
   - 根据你的系统和安装方式，可能需要使用不同的命令来重新启动服务器。

3. 测试 SSL 连接：
   - 使用浏览器访问你的网站，确保连接是通过 HTTPS 进行的。浏览器地址栏中应该显示一个锁图标，表示连接是安全的。
   - 你可以使用在线工具如 [SSL Labs](https://www.ssllabs.com/) 来测试你的 SSL 配置的安全性和兼容性。

**注意事项**：

- 在配置 SSL 时，确保证书和私钥文件的安全性，防止未经授权的访问。
- 定期更新 SSL 证书，以确保安全性和兼容性。
- 如果你的网站使用多个域名或子域名，你可能需要为每个域名申请和安装单独的 SSL 证书，或者使用通配符证书或多域名证书。
- 在配置强制 SSL 时，要注意可能会影响一些旧的链接或脚本，确保你的网站在 HTTPS 下正常工作。

### 二、如果没有mod_ssl.so

如果你的 Apache 服务器中 SSL 模块不存在，可以按照以下步骤来解决：

**一、确认 Apache 版本支持 SSL**

首先，确保你安装的 Apache 版本支持 SSL。大多数现代版本的 Apache 都支持 SSL，但如果你的版本非常旧，可能需要升级。

**二、安装 SSL 模块**

1. 在 Linux 系统中，不同的发行版安装方式可能略有不同。以下是一些常见的方法：

   - **CentOS/RHEL**：
     - 使用 yum 安装：
       ```
       sudo yum install mod_ssl
       ```

   - **Ubuntu/Debian**：
     - 使用 apt-get 安装：
       ```
       sudo apt-get install libapache2-mod-ssl
       ```

2. 安装过程中可能会提示你确认一些依赖项和配置选项，按照提示进行操作。

**三、检查模块是否安装成功**

1. 安装完成后，可以通过以下方法检查 SSL 模块是否已成功安装：

   - 查看 Apache 模块目录：在 Linux 系统中，模块通常位于 `/etc/httpd/modules`（对于 CentOS/RHEL）或 `/usr/lib/apache2/modules`（对于 Ubuntu/Debian）。确认是否有 `mod_ssl.so` 文件存在。

   - 检查 Apache 配置文件：打开 Apache 的主配置文件（通常是 `/etc/httpd/conf/httpd.conf` 或 `/etc/apache2/apache2.conf`），查找是否有类似于 `LoadModule ssl_module modules/mod_ssl.so` 的行。如果有，说明模块已被加载。

2. 如果模块已安装但未被加载，可以手动在配置文件中添加以下行来加载 SSL 模块：

   ```
   LoadModule ssl_module modules/mod_ssl.so
   ```

**四、重新启动 Apache 服务器**

安装和配置 SSL 模块后，需要重新启动 Apache 服务器以使更改生效：

- CentOS/RHEL：
  ```
  sudo systemctl restart httpd
  ```

- Ubuntu/Debian：
  ```
  sudo service apache2 restart
  ```

这样，你的 Apache 服务器就应该能够支持 SSL 了。



------

8、



------

防火墙快速批处理

```bash
systemctl start firewalld
systemctl enable firewalld
firewall-cmd --permanent --zone=public --add-interface=eth0
firewall-cmd --permanent --add-port=22/tcp --add-port=80/tcp --add-port=443/tcp --add-port=53/tcp --add-port=53/udp
systemctl reload firewalld
systemctl restart firewalld
firewall-cmd --list-all

```

本地yum源配置

```bash
mkdir /media/cdrom/
mount /dev/cdrom /media/cdrom/
cd /etc/yum.repos.d/
mv openEuler.repo openEuler.repo.bak
vim /etc/yum.repos.d/local.repo
[local]
name=localyum
baseurl=file:///media/cdrom
gpgcheck=0
enabled=1
```

```bash
#永久自动挂载yum源
#编辑配置文件
vi /etc/fstab
#最后加上一条
/dev/cdrom /media/cdrom iso9660 defaults 0 0
或
mount /kvm/openeuler.iso /media/cdrom/
#重新挂载
mount -a
#检查是否挂载成功
df -h
```

网卡相关

```bash
安装支持重启网卡命令包

yum -y install network-scripts

网卡重启

systemctl restart network

网卡重启（先卸载，再加载）

nmcli c load /etc/sysconfig/network-scripts/ifcfg-ens33

nmcli c up /etc/sysconfig/network-scripts/ifcfg-ens33


```

正向域配置文件

```bash
$TTL 1D
@       IN SOA  @ rname.invalid. (
                                        0       ; serial
                                        1D      ; refresh
                                        1H      ; retry
                                        1W      ; expire
                                        3H )    ; minimum
        NS      @
        A       127.0.0.1
        AAAA    ::1
        PTR     localhost.
xckt1   PTR     192.168.239.129
xckt2   PTR     192.168.239.130
xckt3   PTR     192.168.239.131 
```

反向域配置文件

```bash
$TTL 1D
@       IN SOA  @ rname.invalid. (
                                        0       ; serial
                                        1D      ; refresh
                                        1H      ; retry
                                        1W      ; expire
                                        3H )    ; minimum
        NS      @
        A       127.0.0.1
        AAAA    ::1
129     A       ecs1.xckt.com.
130     A       ecs2.xckt.com.
131     A       ecs3.xckt.com.
```

firewall-cmd --zone=public --add-port=xx/tcp --add-port=xx/udp

```
服务对应端口号（部分）
	DNS服务：53/tcp、53/udp
	chrony服务：123/udp
	Apache服务：80/tcp、443/tcp
	tomcat服务：80/tcp、443/tcp、8080/tcp
	nfs服务：2049/tcp、111/tcp、111/utp
	smaba服务：139/tcp、445/tcp
	iscs服务：860/tcp、3260/tcp
	postgresql服务：5432/tcp、5432/udp
```
