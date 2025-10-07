# MySQL临时密码获取失败解决办法

> 适用于官方源安装的MySQL软件包

你的 MySQL 服务启动失败，并且 `/var/log/mysqld.log` 中没有生成临时密码，可能是因为数据目录 (`/var/lib/mysql`) 中存在残留文件，导致初始化失败。以下是解决方法：

---

### **1. 清理残留数据并重新初始化**
#### （1）停止 MySQL（如果正在运行）
```bash
systemctl stop mysqld
```

#### （2）删除旧的 MySQL 数据目录
```bash
rm -rf /var/lib/mysql/*
```
#### （3）重新初始化 MySQL
```bash
mysqld --initialize --user=mysql
```
- 此命令会生成新的临时密码，并记录在 `/var/log/mysqld.log` 中。

#### （4）检查临时密码
```bash
grep 'temporary password' /var/log/mysqld.log
```
输出示例：
```
2025-07-15T10:00:00.000000Z 1 [Note] A temporary password is generated for root@localhost: abc123xyz
```

#### （5）启动 MySQL 服务
```bash
systemctl start mysqld
```

---

### **2. 如果仍无法启动**
#### （1）检查错误日志
```bash
tail -n 50 /var/log/mysqld.log
```
常见问题：
- **端口冲突**：检查是否有其他 MySQL 或 MariaDB 进程占用 `3306` 端口。
- **权限问题**：确保 `/var/lib/mysql` 属于 `mysql` 用户：
  ```bash
  chown -R mysql:mysql /var/lib/mysql
  ```
- **SELinux 阻止**：临时禁用 SELinux 测试：
  ```bash
  setenforce 0
  ```

#### （2）修复后重新启动
```bash
systemctl restart mysqld
```

---

### **3. 完成初始化**
成功启动后，运行安全配置脚本：
```bash
mysql_secure_installation
```
按提示操作：
1. 输入临时密码。
2. 设置新密码。
3. 移除匿名用户、禁止远程 root 登录等。

---

### **4. 其他可能的问题**
- **如果 `mysqld --initialize` 失败**：  
  确保已安装 `mysql-community-server` 并尝试重新安装：
  ```bash
  yum reinstall mysql-community-server
  ```

- **如果日志中无临时密码**：  
  可能是初始化未完成，手动指定日志路径：
  ```bash
  mysqld --initialize --user=mysql --console
  ```
  观察终端输出的临时密码。

---

### **总结步骤**
```bash
# 1. 停止服务并清理数据
systemctl stop mysqld
rm -rf /var/lib/mysql/*

# 2. 重新初始化
mysqld --initialize --user=mysql
chown -R mysql:mysql /var/lib/mysql

# 3. 启动服务并获取密码
systemctl start mysqld
grep 'temporary password' /var/log/mysqld.log

# 4. 安全配置
mysql_secure_installation
```

如果问题依旧，请提供 `/var/log/mysqld.log` 中的错误内容以便进一步排查。