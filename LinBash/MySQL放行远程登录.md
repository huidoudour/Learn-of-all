你遇到的这个错误提示表明你输入的SQL语句在语法上存在问题。在MySQL 8.0及更高版本中，授予用户权限并设置密码的语法有所改变，对于你想要实现的给`root`用户授予所有权限并允许从任意远程主机（`%`表示任意远程主机）访问且设置密码的操作，正确的语法如下：

### 针对MySQL 8.0及更高版本
```sql
CREATE USER 'root'@'%' IDENTIFIED WITH caching_sha2_password BY 'your_password';
#或
ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'your_password';

GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

这里分三步来完成：
1. **创建用户并设置密码**：
    - 首先使用`CREATE USER`语句来创建`root`用户在指定主机（这里是`%`，即任意远程主机）上的账号，并通过`IDENTIFIED WITH caching_sha2_password BY '123456'`来设置密码。`caching_sha2_password`是MySQL 8.0及更高版本默认的密码验证插件。
2. **授予权限**：
    - 接着使用`GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION`语句来授予`root`用户在所有数据库和所有表上的所有权限，并且允许该用户将自己所拥有的权限授予其他用户。
3. **刷新权限表**：
    - 最后执行`FLUSH PRIVILEGES`语句来刷新MySQL的权限表，使前面创建用户和授予权限的操作生效。

### 针对MySQL 5.7及以下版本
如果你的MySQL版本是5.7及以下，那么可以使用以下语法来实现相同的功能：
```sql
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '123456' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

不过从你之前报错来看，你的MySQL版本应该是8.0及更高版本，所以建议按照前面针对该版本给出的正确语法来操作。操作完成后，记得检查下防火墙等网络设置是否允许外部主机访问MySQL服务使用的端口（默认是3306端口），以确保能够正常从远程进行连接。