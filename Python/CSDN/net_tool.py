import subprocess
import sys
import os
import time
from ctypes import windll, create_unicode_buffer
import win32wnet
import win32netcon
from PyQt5.QtWidgets import (QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, 
                             QLabel, QLineEdit, QPushButton, QGroupBox, QCheckBox, 
                             QMessageBox, QComboBox, QSpacerItem, QSizePolicy)
from PyQt5.QtCore import Qt, QSize
from PyQt5.QtGui import QIcon, QFont, QPixmap, QPainter

class DriveMapperApp(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("网络驱动器映射工具")
        self.setWindowIcon(self.emoji_icon("🔗"))
        self.setFixedSize(500, 500)  # 稍微增大窗口尺寸
        
        # 主窗口部件
        self.main_widget = QWidget()
        self.setCentralWidget(self.main_widget)
        
        # 主布局
        self.main_layout = QVBoxLayout()
        self.main_layout.setContentsMargins(20, 20, 20, 20)  # 设置边距
        self.main_layout.setSpacing(15)  # 设置控件间距
        self.main_widget.setLayout(self.main_layout)
        
        # 初始化UI
        self.init_ui()
        
    def emoji_icon(self, emoji):
        """创建emoji图标"""
        pixmap = QPixmap(32, 32)
        pixmap.fill(Qt.transparent)
        
        painter = QPainter(pixmap)
        font = painter.font()
        font.setPointSize(20)
        painter.setFont(font)
        painter.drawText(pixmap.rect(), Qt.AlignCenter, emoji)
        painter.end()
        
        return QIcon(pixmap)
    
    def init_ui(self):
        """初始化用户界面"""
        # 标题
        title = QLabel("网络驱动器映射工具")
        title.setFont(QFont("Microsoft YaHei", 16, QFont.Bold))
        title.setAlignment(Qt.AlignCenter)
        title.setStyleSheet("margin-bottom: 15px;")
        self.main_layout.addWidget(title)
        
        # 连接设置组
        connection_group = QGroupBox("⚡ 连接设置")
        connection_group.setFont(QFont("Microsoft YaHei", 10))
        connection_layout = QVBoxLayout()
        connection_layout.setSpacing(12)  # 组内控件间距
        connection_layout.setContentsMargins(15, 15, 15, 15)  # 组内边距
        
        # 服务器IP
        ip_layout = QHBoxLayout()
        ip_label = QLabel("🖥️ 服务器IP:")
        ip_label.setFixedWidth(100)  # 固定标签宽度
        ip_layout.addWidget(ip_label)
        self.ip_input = QLineEdit("")
        self.ip_input.setPlaceholderText("例如: 192.168.1.100")
        self.ip_input.setStyleSheet("padding: 5px;")
        ip_layout.addWidget(self.ip_input)
        connection_layout.addLayout(ip_layout)
        
        # 共享文件夹
        share_layout = QHBoxLayout()
        share_label = QLabel("📁 共享文件夹:")
        share_label.setFixedWidth(100)
        share_layout.addWidget(share_label)
        self.share_input = QLineEdit("")
        self.share_input.setPlaceholderText("例如: SharedFolder")
        self.share_input.setStyleSheet("padding: 5px;")
        share_layout.addWidget(self.share_input)
        connection_layout.addLayout(share_layout)
        
        # 驱动器盘符
        drive_layout = QHBoxLayout()
        drive_label = QLabel("💽 驱动器盘符:")
        drive_label.setFixedWidth(100)
        drive_layout.addWidget(drive_label)
        self.drive_combo = QComboBox()
        self.drive_combo.addItems([f"{chr(i)}:" for i in range(90, 64, -1)])
        self.drive_combo.setCurrentText("")
        self.drive_combo.setStyleSheet("padding: 5px;")
        drive_layout.addWidget(self.drive_combo)
        connection_layout.addLayout(drive_layout)
        
        # 账户信息
        user_layout = QHBoxLayout()
        user_label = QLabel("👤 用户名:")
        user_label.setFixedWidth(100)
        user_layout.addWidget(user_label)
        self.user_input = QLineEdit("")
        self.user_input.setPlaceholderText("例如: administrator")
        self.user_input.setStyleSheet("padding: 5px;")
        user_layout.addWidget(self.user_input)
        connection_layout.addLayout(user_layout)
        
        pwd_layout = QHBoxLayout()
        pwd_label = QLabel("🔑 密码:")
        pwd_label.setFixedWidth(100)
        pwd_layout.addWidget(pwd_label)
        self.pwd_input = QLineEdit("")
        self.pwd_input.setPlaceholderText("输入密码")
        self.pwd_input.setEchoMode(QLineEdit.Password)
        self.pwd_input.setStyleSheet("padding: 5px;")
        pwd_layout.addWidget(self.pwd_input)
        connection_layout.addLayout(pwd_layout)
        
        # 持久化选项
        self.persistent_check = QCheckBox("保持持久连接 (重启后自动重新连接)")
        self.persistent_check.setChecked(True)
        self.persistent_check.setStyleSheet("margin-top: 10px;")
        connection_layout.addWidget(self.persistent_check)
        
        connection_group.setLayout(connection_layout)
        self.main_layout.addWidget(connection_group)
        
        # 按钮区域
        button_layout = QHBoxLayout()
        button_layout.setSpacing(20)  # 按钮间距
        
        # 添加弹性空间使按钮居中
        button_layout.addItem(QSpacerItem(20, 20, QSizePolicy.Expanding, QSizePolicy.Minimum))
        
        # 映射按钮
        self.map_button = QPushButton(" 映射驱动器")
        self.map_button.setIcon(self.emoji_icon("🗺️"))
        self.map_button.setFixedSize(150, 40)  # 固定按钮大小
        self.map_button.setStyleSheet("""
            QPushButton {
                background-color: #4CAF50;
                color: white;
                border-radius: 5px;
                padding: 8px;
                font-weight: bold;
            }
            QPushButton:hover {
                background-color: #45a049;
            }
        """)
        self.map_button.clicked.connect(self.map_drive)
        button_layout.addWidget(self.map_button)
        
        # 清理按钮
        self.clean_button = QPushButton(" 清理连接")
        self.clean_button.setIcon(self.emoji_icon("🧹"))
        self.clean_button.setFixedSize(150, 40)
        self.clean_button.setStyleSheet("""
            QPushButton {
                background-color: #f44336;
                color: white;
                border-radius: 5px;
                padding: 8px;
                font-weight: bold;
            }
            QPushButton:hover {
                background-color: #d32f2f;
            }
        """)
        self.clean_button.clicked.connect(self.clean_connections)
        button_layout.addWidget(self.clean_button)
        
        button_layout.addItem(QSpacerItem(20, 20, QSizePolicy.Expanding, QSizePolicy.Minimum))
        
        self.main_layout.addLayout(button_layout)
        
        # 状态栏
        self.status_bar = QLabel("🟢 就绪")
        self.status_bar.setAlignment(Qt.AlignCenter)
        self.status_bar.setStyleSheet("""
            color: #666;
            margin-top: 10px;
            padding: 8px;
            background-color: #f5f5f5;
            border-radius: 5px;
        """)
        self.main_layout.addWidget(self.status_bar)
        
    def run_cmd(self, command):
        """执行命令并返回输出"""
        try:
            result = subprocess.run(
                command,
                shell=True,
                check=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                encoding='gbk',
                text=True
            )
            return result.stdout.strip()
        except subprocess.CalledProcessError as e:
            self.status_bar.setText(f"🔴 错误: {e.stderr}")
            return None
    
    def nuclear_cleanup(self, server_ip):
        """彻底清除所有可能的残留连接"""
        self.status_bar.setText("🧽 正在深度清理...")
        QApplication.processEvents()
        
        self.run_cmd("net use * /delete /y")
        self.run_cmd(f"net use \\\\{server_ip} /delete /y")
        
        creds = self.run_cmd("cmdkey /list")
        if creds and server_ip in creds:
            self.run_cmd(f"cmdkey /delete:\\\\{server_ip}")
            self.run_cmd(f"cmdkey /delete:WindowsLive:target=\\\\{server_ip}")
        
        try:
            windll.mpr.WNetCancelConnection2W(create_unicode_buffer(f"\\\\{server_ip}"), 0, True)
            win32wnet.WNetCancelConnection2(f"\\\\{server_ip}", 0, True)
        except Exception as e:
            self.status_bar.setText(f"🔴 API清理错误: {e}")
        
        self.status_bar.setText("🔄 正在重启网络服务...")
        QApplication.processEvents()
        self.run_cmd("net stop workstation /y")
        time.sleep(2)
        self.run_cmd("net start workstation")
        time.sleep(1)
        
        self.status_bar.setText("🟢 清理完成")
    
    def clean_connections(self):
        """清理所有网络连接"""
        server_ip = self.ip_input.text().strip()
        if not server_ip:
            QMessageBox.warning(self, "警告", "请输入服务器IP地址")
            return
            
        reply = QMessageBox.question(
            self, '确认',
            '确定要清理所有网络连接吗？这可能会断开现有的网络驱动器连接。',
            QMessageBox.Yes | QMessageBox.No, QMessageBox.No
        )
        
        if reply == QMessageBox.Yes:
            self.nuclear_cleanup(server_ip)
            QMessageBox.information(self, "完成", "网络连接已清理完成")
    
    def map_drive(self):
        """映射网络驱动器"""
        server_ip = self.ip_input.text().strip()
        share = self.share_input.text().strip()
        drive = self.drive_combo.currentText()
        user = self.user_input.text().strip()
        pwd = self.pwd_input.text()
        
        if not all([server_ip, share, drive, user, pwd]):
            QMessageBox.warning(self, "警告", "请填写所有必填字段")
            return
            
        path = f"\\\\{server_ip}\\{share}"
        persistent = "/persistent:yes" if self.persistent_check.isChecked() else ""
        
        self.status_bar.setText("🔄 正在准备映射...")
        QApplication.processEvents()
        
        self.nuclear_cleanup(server_ip)
        
        self.status_bar.setText(f"🔄 正在映射 {path} 到 {drive}...")
        QApplication.processEvents()
        
        result = self.run_cmd(f"net use {drive} {path} {pwd} /user:{user} {persistent}")
        
        if result:
            self.status_bar.setText(f"🟢 成功映射 {path} 到 {drive}")
            QMessageBox.information(self, "成功", f"网络驱动器已成功映射到 {drive}")
            
            test_result = self.run_cmd(f"dir {drive}")
            if test_result:
                self.status_bar.setText(f"🟢 访问测试成功: {drive} 驱动器内容可读")
            else:
                self.status_bar.setText(f"🟡 映射成功但访问测试失败")
        else:
            self.status_bar.setText("🔴 映射失败")
            QMessageBox.critical(
                self, "错误", 
                "驱动器映射失败！\n\n"
                "请尝试以下解决方案：\n"
                "1. 手动执行清理操作\n"
                "2. 重启计算机后重试\n"
                "3. 检查服务器端的共享权限设置"
            )

if __name__ == "__main__":
    app = QApplication(sys.argv)
    app.setStyle('Fusion')  # 使用Fusion风格使界面更现代
    window = DriveMapperApp()
    window.show()
    sys.exit(app.exec_())

#个人使用，搬运源码
#原文作者：Clay_K
#转载请注明出处
#原文链接：https://blog.csdn.net/Clay_K/article/details/148615836?spm=1001.2014.3001.5501