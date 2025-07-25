以下是几种在 openEuler 操作系统上安装 GUI 的方法：

### 安装 UKUI 图形界面
1. **安装字体文件**：在终端中输入命令 `yum groupinstall fonts -y`，等待安装完成.
2. **安装 UKUI**：执行命令 `yum install ukui -y` 进行 UKUI 的安装.
3. **设置默认图形开机**：输入 `systemctl set-default graphical.target` 设置默认图形界面启动.
4. **重启系统**：使用 `reboot` 命令重启电脑，即可看到图形界面.

### 安装深度桌面环境 DDE
1. **更新系统**：在终端输入 `dnf update`，将系统中所有已安装的软件包更新到最新版本.
2. **安装 DDE**：执行命令 `dnf -y install dde` 开始下载并安装深度桌面环境，此过程需耐心等待，安装完成后约占2.5G的磁盘空间.
3. **设置默认启动模式**：输入 `systemctl set-default graphical.target`，将系统默认运行级别设置为图形用户界面模式，然后使用 `reboot` 命令重启系统，使设置生效.

### 离线安装 UKUI 图形界面
1. **下载安装镜像和相关依赖**：从官网下载 openEuler 的安装镜像 iso 文件，然后找一台可联网的虚拟机，使用 `dnf download --resolve ukui` 命令下载 UKUI 及其相关依赖，不同版本的离线包有所不同，需根据实际情况进行下载.
2. **导入 rpm 包并赋权**：使用 ssh 将下载好的 rpm 包导入对应的无 gui 的离线系统中，然后执行命令 `chmod 644 *.rpm` 给所有 rpm 包赋予权限.
3. **安装 rpm 包**：切换到源路径 `/etc/yum.repos.d/`，将原有的源文件重命名为 `*.repo.bak` 以禁用源，避免 dnf 优先连源导致无限等待，接着执行命令 `dnf -y install./*.rpm --skip-broken` 进行一键安装.
4. **设置图形化自启动**：执行命令 `systemctl set-default graphical.target`，将图形化界面设置为默认自启动.
5. **重启系统**：使用 `reboot` 命令重启系统，完成离线安装图形界面的操作.

------

1. **安装图形界面组件（以UKUI为例）**
   - 首先，安装字体文件，在终端中输入命令`yum groupinstall fonts -y`。这一步是为了确保系统有合适的字体来显示图形界面中的文字等内容。
   - 然后安装UKUI，执行命令`yum install ukui -y`。安装完成后，图形界面相关的文件和程序就已经在系统中了。
2. **启动图形界面**
   - 当你需要启动图形界面时，在终端中输入`startx`命令。`startx`命令是一个用于启动X Window系统（图形界面的底层系统）的脚本。它会读取相关的配置文件，启动图形服务器和窗口管理器等组件，从而开启图形界面。
   - 不过要注意，如果你在字符界面下已经启动了一些服务或者程序，启动图形界面后可能会出现资源竞争等情况。而且这种手动启动图形界面的方式，每次启动后只会开启一个图形会话，当你关闭图形界面（例如通过图形界面中的关机或注销选项），系统会回到字符界面，下次需要图形界面时仍要手动执行`startx`命令。