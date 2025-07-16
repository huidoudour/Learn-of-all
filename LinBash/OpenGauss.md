# OpenGauss

1. 在ecs2安装open gauss数据库及配套工具，初始化数据库并设置ROOT密码为:Admin@123。

```bash
yum -y install opengauss
su - opengauss
gs_ctl start -D /var/lib/opengauss/data -Z single_node
ps ux #查看信息
[opengauss@ecs2 ~]$ vim /var/lib/opengauss/data/postgresql.conf

110行 password_encryption_type = 0 
#取消注释，2改为0
```

```sql
gsql -d postgres -p 7654 -r				#进入数据库初始化

alter user opengauss with password 'Admin@123';
#更改用户opengauss(root超级管理员)密码
\q		#退出
```

2. 登录postgres数据库，创建一个用户密码为：user_test/Admin@123，同时创建数据库实例名为：db_test，创建模式为sche_test，完成后创建下表，表名为：tb_test.。

   | c_customer_sk | integer |
   | ------------- | ------- |
   | c_customer_id | char(5) |
   | c_first_name  | char(6) |
   | c_last_name   | char(8) |
   | Amount        | intger  |

```bash
su - opengauss
gsql -d postgres -p 7654 -r	
```

```sql
create user user_test with password 'Admin@123';		#创建用户/密码

create database db_test;		#创建数据库
\c db_test;						#连接数据库
create schema sche_test;		#创建数据库的模式为sche_test

#创建表格
create table sche_test.tb_test (
c_customer_sk integer,
c_customer_id char(5),
c_first_name char(6),
c_last_name char(8),
Amount integer
);

```

3. 撤销user_test用户的sysadmin权限和LOGIN权限，将sche_test的使用权限和表tb_test的所有权限授权给用户user_test。

```bash
gsql -d postgres -p 7654 -r			
su - opengauss
\c db_test;	
```

```sql
db_test=# <↓>
alter user user_test nologin;
alter user user_test nosysadmin;
#撤销sysadmin权限和login权限

grant all privileges on table sche_test.tb_test to user_test;

\dp sche_test.tb_test;		#查看信息
```



```
代码回收站
gs_initdb -D /var/lib/opengauss/data -root -Admin@123
```



