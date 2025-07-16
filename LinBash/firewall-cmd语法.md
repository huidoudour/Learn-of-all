`firewall-cmd` 是一个用于管理 `firewalld`（动态防火墙管理器）命令行工具。它可以用来配置和管理 Linux 系统的防火墙规则。`--permanent` 选项用于在防火墙规则之间添加持久性更改，即使在重启或重新加载防火墙服务后这些规则依然有效。

### `firewall-cmd --permanent --add-` 语法

`firewall-cmd --permanent --add-` 后面通常会跟相应的参数，用于添加不同的规则。以下是一些常用的命令示例，以及相关的参数和选项说明。

### 基本用法

1. **添加服务**：
   ```bash
   firewall-cmd --permanent --add-service=<service_name>
   ```
   例如，允许 HTTP 服务（端口 80）：
   ```bash
   firewall-cmd --permanent --add-service=http
   ```

2. **添加端口**：
   ```bash
   firewall-cmd --permanent --add-port=<port>/<protocol>
   ```
   例如，允许 TCP 端口 8080：
   ```bash
   firewall-cmd --permanent --add-port=8080/tcp
   ```

3. **添加源地址**（允许特定 IP 地址或地址段）：
   ```bash
   firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="<IP>" accept'
   ```
   例如，允许来自 IP 地址 `192.168.1.100` 的连接：
   ```bash
   firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="192.168.1.100" accept'
   ```

4. **添加信任的接口**：
   ```bash
   firewall-cmd --permanent --zone=<zone> --add-interface=<interface>
   ```
   例如，将 `eth0` 接口添加到 `trusted` 区域：
   ```bash
   firewall-cmd --permanent --zone=trusted --add-interface=eth0
   ```

### 最后步骤

在对防火墙进行任何永久更改后，别忘了重新加载防火墙，以使更改生效：
```bash
firewall-cmd --reload
```

### 检查当前设置

您可以使用以下命令来检查当前的防火墙设置：
```bash
firewall-cmd --list-all
```

### 参考

- `service_name`：可以使用 `firewall-cmd --get-services` 命令查看可用服务。
- `port/protocol`：例如，`80/tcp` 表示 TCP 协议的 80 端口。

### 小结

使用 `firewall-cmd --permanent --add-` 命令行工具可以轻松地管理和配置 Linux 防火墙的规则，确保您按照安全需求进行适当的配置。如果您有特定需求或问题，请提供更多信息以便获得更加详细的帮助！





在Linux系统中启用防火墙后，如果发现`nslookup`命令无法使用或者无法通过SSH连接到服务器，这通常是因为防火墙规则阻止了必要的网络流量。要解决这个问题，你需要配置防火墙以允许这些服务所需的流量通过。

以下是一些基本的步骤来配置防火墙（以`firewalld`为例，这是许多现代Linux发行版使用的防火墙管理工具）：

1. **检查防火墙状态**：
   首先，检查防火墙是否正在运行以及哪些服务被允许。

   ```bash
   sudo firewall-cmd --state
   sudo firewall-cmd --list-all
   ```

2. **允许DNS流量**：
   `nslookup`命令用于查询DNS服务器以解析域名。如果`nslookup`无法使用，可能是因为防火墙阻止了DNS流量（通常是UDP和TCP的53端口）。

   ```bash
   sudo firewall-cmd --permanent --add-service=dns
   sudo firewall-cmd --reload
   ```

3. **允许SSH流量**：
   SSH服务通常使用TCP的22端口。如果无法通过SSH连接到服务器，你需要确保防火墙允许这个端口的流量。

   ```bash
   sudo firewall-cmd --permanent --add-service=ssh
   sudo firewall-cmd --reload
   ```

4. **如果服务不在默认列表中**：
   如果你使用的服务不在`firewalld`的默认服务列表中，你需要手动开放端口。例如，如果你的SSH服务运行在非标准端口上，你需要这样做：

   ```bash
   sudo firewall-cmd --permanent --add-port=2222/tcp  # 替换2222为你的SSH端口
   sudo firewall-cmd --reload
   ```

5. **验证更改**：
   重新加载防火墙规则后，验证更改是否生效。

   ```bash
   sudo firewall-cmd --list-all
   ```

6. **其他防火墙工具**：
   如果你使用的是`iptables`或`ufw`（在Ubuntu上常见），你需要使用相应的命令来允许流量。例如，使用`ufw`：

   ```bash
   sudo ufw allow ssh
   sudo ufw allow dns
   sudo ufw reload
   ```

   或者使用`iptables`：

   ```bash
   sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT
   sudo iptables -A INPUT -p udp --dport 53 -j ACCEPT
   sudo iptables -A INPUT -p tcp --dport 53 -j ACCEPT
   sudo iptables-save > /etc/iptables/rules.v4
   ```

请根据你使用的Linux发行版和防火墙管理工具选择合适的命令。确保在更改防火墙设置时，你可以通过其他方式访问服务器（例如，通过控制台或物理访问），以防止被锁定。





