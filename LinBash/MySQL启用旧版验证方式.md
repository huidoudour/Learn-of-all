# MySQL对PHP旧版本请求使用旧版本验证方式

启用旧版验证方式

```mysql
vim /etc/my.inf			#加入中括号下面两行
[mysqld]
plugin-load-add = auth_socket.so
mysql_native_password = FORCE_PLUS_PERMANENT
```

重启MySQL

```bash
systemctl restart mysqld
```

登录MySQL并查询当前用户(wordpress)使用的验证方式

```bash
mysql -uroot -p
```

```mysql
select plugin_name ,plugin_status from information_schema.plugins where plugin_name like '%native%' or plugin_name like '%socket%';

#输出示例
mysql> select plugin_name ,plugin_status from information_schema.plugins where plugin_name like '%native%' or plugin_nam
e like '%socket%';
+-----------------------+---------------+
| plugin_name           | plugin_status |
+-----------------------+---------------+
| mysql_native_password | DISABLED      |
+-----------------------+---------------+
1 row in set (0.00 sec)
```

```mysql
#变更验证方式
alter user 'wp'@'%' identified with mysql_native_password by '<严格密码>';
#刷新权限
flush idenditied;
```

