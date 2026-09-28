# RepoManager X11 渲染使用指南

## 原理说明

Swing 程序需要 X11 显示服务才能渲染 GUI。在无桌面环境的 Linux 上，通过 `xinit` 启动 X 服务器并直接运行 Java 程序，使程序独占 TTY 显示。

```
xinit
  ├── 启动 X.Org Server（显卡驱动 → 显示器）
  ├── 运行 Java/Swing 程序
  │     └── X11 协议 ←→ X Server ←→ 屏幕渲染
  └── 程序退出 → 关闭 X Server → 回到 TTY
```

---

## 一、安装 X.Org（仅需一次）

```bash
sudo apt update
sudo apt install xorg xinit
```

验证安装：

```bash
X -version          # 查看 X Server 版本
which xinit         # 确认 xinit 命令存在
```

---

## 二、基本运行命令

### 标准启动

```bash
cd /home/huidou/java
xinit /bin/sh -c 'java RepoManager.java' -- :1 vt1
```

参数说明：

| 参数 | 含义 |
|---|---|
| `xinit` | 启动 X Server 并运行指定程序 |
| `/bin/sh -c '...'` | 在 X 中执行的命令 |
| `java RepoManager.java` | JDK 21 直接源码运行 |
| `--` | 分隔符，后面的参数传给 X Server |
| `:1` | 使用第 1 号显示（`:0` 通常被占用） |
| `vt1` | 在当前 TTY (tty1) 上显示 |

### 带编码指定

```bash
xinit /bin/sh -c 'java -Dfile.encoding=UTF-8 RepoManager.java' -- :1 vt1
```

### 重定向日志（排错用）

```bash
xinit /bin/sh -c 'java RepoManager.java > /tmp/repo.log 2>&1' -- :1 vt1
cat /tmp/repo.log
```

---

## 三、TTY 切换与进程管理

### TTY 间切换

```
Ctrl + Alt + F1   ← 切换到 tty1（程序运行在这里）
Ctrl + Alt + F2   ← 切换到 tty2（终端）
Ctrl + Alt + F3   ← 切换到 tty3
...
Alt + F1~F6       ← 部分系统无需 Ctrl
```

### 在另一个 TTY 中查看/终止进程

```bash
# 切换到 tty2（Ctrl+Alt+F2），登录后执行
ps aux | grep RepoManager
kill <PID>

# 或一行杀死
pkill -f RepoManager
```

---

## 四、DISPLAY 变量详解

`xinit` 会自动设置 `DISPLAY=:1` 环境变量。也可手动指定：

```bash
# 查看当前 DISPLAY
echo $DISPLAY

# 手动指定（调试时用）
export DISPLAY=:1
java RepoManager.java
```

DISPLAY 格式：`<主机>:<显示编号>.<屏幕编号>`

| 值 | 含义 |
|---|---|
| `:0` | 本地第 0 号显示（通常已有桌面环境） |
| `:1` | 本地第 1 号显示（xinit 常用） |
| `localhost:10.0` | X11 转发（SSH -X） |

---

## 五、分辨率调整

默认分辨率可能不合适，创建 X 配置文件：

```bash
sudo vim /usr/share/X11/xorg.conf.d/10-resolution.conf
```

```conf
Section "Screen"
    Identifier "Screen0"
    SubSection "Display"
        Depth 24
        Modes "1024x768" "1280x720" "1920x1080"
    EndSubSection
EndSection
```

或在启动时用 `xrandr` 调整（需先有 X）：

```bash
# 先启动 X 但不运行 Java
xinit /bin/sh -c 'xrandr -s 1024x768 && java RepoManager.java' -- :1 vt1
```

---

## 六、鼠标支持

TTY 下 X 默认启用鼠标。如果 VM 没有鼠标指针：

1. **键盘操作**：
   - `Tab` — 切换焦点
   - `Space/Enter` — 点击按钮
   - `方向键` — 列表选择

2. **启用鼠标集成**（VMware/VirtualBox）：
   ```bash
   # 检查输入设备
   cat /var/log/Xorg.1.log | grep -i mouse
   ```

---

## 七、完整启动脚本

创建 `run.sh` 一键启动：

```bash
cat > /home/huidou/java/run.sh << 'SCRIPT'
#!/bin/bash
cd /home/huidou/java
xinit /bin/sh -c 'java RepoManager.java' -- :1 vt1
SCRIPT

chmod +x /home/huidou/java/run.sh
# 以后只需：
./run.sh
```

---

## 八、排错命令速查

```bash
# X 服务器日志
cat /home/huidou/.local/share/xorg/Xorg.1.log

# 测试 X 是否正常（需安装 xterm）
sudo apt install xterm
xinit /usr/bin/xterm -- :1 vt1

# 查看 Java 是否可用
java --version

# 检查 DISPLAY 变量
echo $DISPLAY
```
