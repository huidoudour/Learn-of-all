# RepoManager Linux 使用指南

## 环境要求

- **JDK 21+**（支持直接运行 `.java` 源码）
- **Git**（已安装并配置）
- **X.Org**（仅用于 GUI 渲染，无需桌面环境）

---

## 一、安装依赖

```bash
# JDK
sudo apt update
sudo apt install openjdk-21-jdk git

# X.Org 核心（无桌面环境）
sudo apt install xorg xinit

# 中文字体（Swing 中文显示）
sudo apt install fonts-wqy-zenhei fonts-wqy-microhei
sudo fc-cache -fv
```

---

## 二、传输文件

从 Windows 传到 Linux（在 Windows PowerShell 执行）：

```bash
scp RepoManager.java huidou@192.168.128.168:/home/huidou/java/
```

---

## 三、运行方式

### 方式 A：TTY 全屏运行（推荐，无桌面环境）

```bash
cd /home/huidou/java

xinit /bin/sh -c 'cd /home/huidou/java && java RepoManager.java' -- :1 vt1
```

**效果**：当前 TTY 全屏显示 GUI，按 ESC 或点"退出"按钮返回终端。

### 方式 B：SSH + X11 转发（远程窗口）

**Linux 端**无需额外配置，**Windows 端**需安装 X 服务器：

1. 下载安装 [VcXsrv](https://sourceforge.net/projects/vcxsrv/) 或 [MobaXterm](https://mobaxterm.mobatek.net/)
2. xShell 设置：**属性 → 连接 → SSH → 隧道** → 勾选 **"转发 X11 连接"**

然后 SSH 连接后直接运行：

```bash
cd /home/huidou/java
java RepoManager.java
```

界面会作为 Windows 窗口弹出。

---

## 四、退出程序

| 方式 | 操作 |
|---|---|
| **快捷键** | 按 `ESC` 直接退出 |
| **按钮** | 点击右栏右下角的"退出"按钮 |
| **窗口关闭** | `Alt + F4` |
| **强制终止** | `Ctrl + Alt + F2` 切换到其他 TTY → `kill $(pgrep -f RepoManager)` |

---

## 五、数据文件

程序会在**当前工作目录**下生成 `repo.json`：

```json
{
  "repos": [
    "/home/huidou/projects/project-a",
    "/home/huidou/projects/project-b"
  ]
}
```

可手动编辑后重启程序生效。

---

## 六、常见问题

### Q：中文显示为方块

```bash
sudo apt install fonts-wqy-zenhei fonts-wqy-microhei
sudo fc-cache -fv
```

### Q：`xinit` 报错 `cannot open display`

确保命令格式正确，用 `-- :1 vt1` 指定显示编号。

### Q：程序一闪就退出

用重定向查看错误日志：

```bash
xinit /bin/sh -c 'cd /home/huidou/java && java RepoManager.java > /tmp/repo.log 2>&1' -- :1 vt1
cat /tmp/repo.log
```

### Q：`java: command not found`

```bash
# 查找 Java 安装路径
update-alternatives --config java
# 或设置 JAVA_HOME
export PATH=$JAVA_HOME/bin:$PATH
```
