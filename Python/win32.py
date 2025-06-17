import ctypes
import sys
import win32api
import win32file
import win32con
import win32process
import psutil
import threading
from datetime import datetime
from time import sleep
from PyQt5.QtWidgets import (QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, 
                             QListWidget, QPushButton, QLabel, QProgressBar, QTextEdit,
                             QSystemTrayIcon, QMenu, QMessageBox, QStyle, QFrame, QAction,
                             QDialog, QVBoxLayout, QHBoxLayout)
from PyQt5.QtCore import Qt, QThread, pyqtSignal, QTimer
from PyQt5.QtGui import QIcon, QFont, QPalette, QColor

# Define constants
try:
    from winioctlcon import FSCTL_LOCK_VOLUME, FSCTL_DISMOUNT_VOLUME, IOCTL_STORAGE_EJECT_MEDIA
except ImportError:
    FSCTL_LOCK_VOLUME = 0x00090018
    FSCTL_DISMOUNT_VOLUME = 0x00090020
    IOCTL_STORAGE_EJECT_MEDIA = 0x2D4808

class EjectProgressDialog(QDialog):
    """自定义进度对话框"""
    def __init__(self, parent=None, drive_letter=""):
        super().__init__(parent)
        self.setWindowTitle("安全弹出USB驱动器")
        self.setWindowIcon(QIcon.fromTheme('drive-removable-media'))
        self.setWindowFlags(self.windowFlags() & ~Qt.WindowContextHelpButtonHint)
        self.setFixedSize(300, 120)
        
        layout = QVBoxLayout()
        self.setLayout(layout)
        
        # 标题标签
        self.title_label = QLabel(f"正在安全弹出 {drive_letter}:...")
        self.title_label.setAlignment(Qt.AlignCenter)
        layout.addWidget(self.title_label)
        
        # 进度条
        self.progress_bar = QProgressBar()
        self.progress_bar.setRange(0, 100)
        self.progress_bar.setTextVisible(False)
        layout.addWidget(self.progress_bar)
        
        # 状态标签
        self.status_label = QLabel("准备解除占用...")
        self.status_label.setAlignment(Qt.AlignCenter)
        layout.addWidget(self.status_label)
        
        # 取消按钮
        self.cancel_btn = QPushButton("取消")
        self.cancel_btn.clicked.connect(self.reject)
        btn_layout = QHBoxLayout()
        btn_layout.addStretch()
        btn_layout.addWidget(self.cancel_btn)
        btn_layout.addStretch()
        layout.addLayout(btn_layout)
        
        # 设置样式
        self.setStyleSheet("""
            QDialog {
                background-color: #f5f5f5;
            }
            QLabel {
                font-size: 12px;
            }
            QProgressBar {
                border: 1px solid #ccc;
                border-radius: 3px;
                text-align: center;
                height: 12px;
            }
            QProgressBar::chunk {
                background-color: #4CAF50;
                width: 10px;
            }
        """)
    
    def update_progress(self, text, value, max_value):
        """更新进度显示"""
        self.progress_bar.setMaximum(max_value)
        self.progress_bar.setValue(value)
        self.status_label.setText(text)

class WorkerThread(QThread):
    update_progress = pyqtSignal(str, int, int)
    update_process_text = pyqtSignal(str)
    operation_complete = pyqtSignal()
    show_message = pyqtSignal(str, str, str)  # title, message, icon

    def __init__(self, drive_letter, operation_type):
        super().__init__()
        self.drive_letter = drive_letter
        self.operation_type = operation_type  # 'find', 'unlock_and_eject'
        self.running = True

    def run(self):
        try:
            if self.operation_type == 'find':
                self.find_locking_processes()
            elif self.operation_type == 'unlock_and_eject':
                self.unlock_and_eject_drive()
        except Exception as e:
            self.log_message(f"线程错误: {str(e)}")
        finally:
            self.operation_complete.emit()

    def get_timestamp(self):
        """获取当前时间戳，格式为[HH:MM:SS]"""
        return datetime.now().strftime("[%H:%M:%S]")

    def log_message(self, message):
        """记录带时间戳的消息"""
        timestamp = self.get_timestamp()
        self.update_process_text.emit(f"{timestamp} {message}\n")

    def find_locking_processes(self):
        """查找锁定驱动器的进程"""
        self.log_message("🔍 正在获取进程列表...")
        self.update_progress.emit("🔍 正在获取进程列表...", 0, 100)

        try:
            processes = list(psutil.process_iter(['pid', 'name', 'exe', 'cmdline', 'username', 'status']))
            total = len(processes)
        except Exception as e:
            self.log_message(f"❌ 获取进程列表失败: {str(e)}")
            return

        self.log_message(f"📊 找到 {total} 个进程，正在扫描...")
        self.update_progress.emit(f"🔎 正在扫描 0/{total} 进程", 0, total)

        locking_processes = []
        drive_path = f"{self.drive_letter}:\\".lower()

        for i, proc in enumerate(processes):
            if not self.running:
                self.log_message("⏹ 用户取消操作")
                break

            self.update_progress.emit(f"🔎 正在扫描 {i+1}/{total}: {proc.name()}", i+1, total)

            try:
                # 检查打开的文件
                for item in proc.open_files():
                    if not self.running:
                        break
                    if item.path.lower().startswith(drive_path):
                        locking_processes.append({
                            'pid': proc.pid,
                            'name': proc.name(),
                            'exe': proc.exe(),
                            'cmdline': ' '.join(proc.cmdline()),
                            'username': proc.username(),
                            'status': proc.status()
                        })
                        break

                # 检查工作目录
                try:
                    cwd = proc.cwd()
                    if cwd and cwd.lower().startswith(drive_path):
                        locking_processes.append({
                            'pid': proc.pid,
                            'name': proc.name(),
                            'exe': proc.exe(),
                            'cmdline': ' '.join(proc.cmdline()),
                            'username': proc.username(),
                            'status': proc.status()
                        })
                except (psutil.AccessDenied, psutil.NoSuchProcess):
                    pass

            except (psutil.AccessDenied, psutil.NoSuchProcess, psutil.ZombieProcess):
                continue

        if not self.running:
            return

        if not locking_processes:
            self.log_message("✅ 未找到锁定进程")
        else:
            self.log_message(f"⚠️ 找到 {len(locking_processes)} 个锁定进程:")
            self.update_process_text.emit("━" * 80 + "\n")

            for proc in locking_processes:
                self.update_process_text.emit(
                    f"{self.get_timestamp()} 🆔 PID: {proc['pid']}\n"
                    f"{self.get_timestamp()} 📛 名称: {proc['name']}\n"
                    f"{self.get_timestamp()} 📂 路径: {proc['exe']}\n"
                    f"{self.get_timestamp()} 💻 命令: {proc['cmdline']}\n"
                    f"{self.get_timestamp()} 👤 用户: {proc['username']}\n"
                    f"{self.get_timestamp()} 📊 状态: {proc['status']}\n"
                    "━" * 80 + "\n"
                )

        self.update_progress.emit("✅ 扫描完成", total, total)

    def unlock_and_eject_drive(self):
        """解除占用并弹出驱动器"""
        self.log_message("🔓 准备解除占用并弹出...")
        self.update_progress.emit("🔓 准备解除占用并弹出...", 0, 4)

        # 1. 查找并关闭锁定进程
        self.log_message("🔍 正在查找锁定进程...")
        self.update_progress.emit("🔍 正在查找锁定进程...", 1, 4)
        locking_processes = self.get_locking_processes()

        if locking_processes:
            self.log_message(f"⚠️ 找到 {len(locking_processes)} 个锁定进程，尝试关闭...")

            for proc in locking_processes:
                if not self.running:
                    self.log_message("⏹ 用户取消操作")
                    break

                try:
                    p = psutil.Process(proc['pid'])
                    p.terminate()
                    self.log_message(f"✅ 已终止进程: {proc['name']} (PID: {proc['pid']})")
                except Exception as e:
                    self.log_message(f"❌ 终止 {proc['name']} (PID: {proc['pid']}) 失败: {str(e)}")

        if not self.running:
            return

        # 2. 标准解锁方法
        self.log_message("🔓 正在解除占用...")
        self.update_progress.emit("🔓 正在解除占用...", 2, 4)
        drive_path = f"\\\\.\\{self.drive_letter}:"

        try:
            h_volume = win32file.CreateFile(
                drive_path,
                win32con.GENERIC_READ | win32con.GENERIC_WRITE,
                win32con.FILE_SHARE_READ | win32con.FILE_SHARE_WRITE,
                None,
                win32con.OPEN_EXISTING,
                0,
                None
            )

            if h_volume == win32file.INVALID_HANDLE_VALUE:
                self.show_message.emit("错误", "无法打开驱动器", "critical")
                return

            try:
                # 锁定卷
                win32file.DeviceIoControl(
                    h_volume,
                    FSCTL_LOCK_VOLUME,
                    None,
                    None,
                    None
                )

                # 卸载卷
                win32file.DeviceIoControl(
                    h_volume,
                    FSCTL_DISMOUNT_VOLUME,
                    None,
                    None,
                    None
                )

                # 3. 弹出媒体
                self.log_message("🚀 正在弹出驱动器...")
                self.update_progress.emit("🚀 正在弹出驱动器...", 3, 4)
                win32file.DeviceIoControl(
                    h_volume,
                    IOCTL_STORAGE_EJECT_MEDIA,
                    None,
                    None,
                    None
                )

                message = f"✅ 成功解除占用并弹出 {self.drive_letter}:，现在可以安全移除设备"
                self.show_message.emit("成功", message, "information")
                self.log_message(message)

            except Exception as e:
                error_msg = f"❌ 解除占用并弹出失败: {str(e)}"
                self.show_message.emit("错误", error_msg, "critical")
                self.log_message(error_msg)
            finally:
                win32file.CloseHandle(h_volume)

        except Exception as e:
            error_msg = f"❌ 操作失败: {str(e)}"
            self.show_message.emit("错误", error_msg, "critical")
            self.log_message(error_msg)

        self.update_progress.emit("✅ 操作完成", 4, 4)

    def get_locking_processes(self):
        """获取所有锁定驱动器的进程"""
        locking_processes = []
        drive_path = f"{self.drive_letter}:\\".lower()

        try:
            processes = list(psutil.process_iter(['pid', 'name', 'exe', 'cmdline', 'username', 'status']))
            total = len(processes)

            for i, proc in enumerate(processes):
                if not self.running:
                    break

                try:
                    # 检查打开的文件
                    for item in proc.open_files():
                        if not self.running:
                            break
                        if item.path.lower().startswith(drive_path):
                            locking_processes.append({
                                'pid': proc.pid,
                                'name': proc.name(),
                                'exe': proc.exe(),
                                'cmdline': ' '.join(proc.cmdline()),
                                'username': proc.username(),
                                'status': proc.status()
                            })
                            break

                    # 检查工作目录
                    try:
                        cwd = proc.cwd()
                        if cwd and cwd.lower().startswith(drive_path):
                            locking_processes.append({
                                'pid': proc.pid,
                                'name': proc.name(),
                                'exe': proc.exe(),
                                'cmdline': ' '.join(proc.cmdline()),
                                'username': proc.username(),
                                'status': proc.status()
                            })
                    except (psutil.AccessDenied, psutil.NoSuchProcess):
                        pass

                except (psutil.AccessDenied, psutil.NoSuchProcess, psutil.ZombieProcess):
                    continue

        except Exception as e:
            self.log_message(f"❌ 获取进程列表失败: {str(e)}")

        return locking_processes

    def stop(self):
        """停止线程"""
        self.running = False

class USBEjectorPro(QMainWindow):
    def __init__(self):
        super().__init__()
        
        self.worker_thread = None
        self.running = False
        self.progress_dialog = None
        
        self.setWindowTitle("💾 USB 安全弹出")
        self.setGeometry(100, 100, 520, 569)  # Reduced height since we removed log view
        
        # 设置窗口图标
        self.setWindowIcon(QIcon.fromTheme('drive-removable-media'))
        
        # 创建系统托盘图标
        self.create_system_tray()
        
        self.init_ui()
        self.refresh_drives()
        
        # 自动刷新计时器
        self.refresh_timer = QTimer(self)
        self.refresh_timer.timeout.connect(self.refresh_drives)
        self.refresh_timer.start(5000)  # 每5秒刷新一次
        
    def init_ui(self):
        """初始化主界面"""
        main_widget = QWidget()
        self.setCentralWidget(main_widget)
        
        layout = QVBoxLayout()
        main_widget.setLayout(layout)
        
        # 标题
        title_label = QLabel("💾 USB 安全弹出专业版")
        title_label.setStyleSheet("font-size: 18px; font-weight: bold;")
        title_label.setAlignment(Qt.AlignCenter)
        layout.addWidget(title_label)
        
        # 驱动器列表
        drive_group = QWidget()
        drive_layout = QVBoxLayout()
        drive_group.setLayout(drive_layout)
        
        drive_label = QLabel("💾 可移动驱动器")
        drive_label.setStyleSheet("font-weight: bold;")
        drive_layout.addWidget(drive_label)
        
        self.drive_list = QListWidget()
        self.drive_list.setStyleSheet("""
            QListWidget {
                font-family: monospace;
                border: 1px solid #c0c0c0;
                border-radius: 4px;
                padding: 2px;
            }
        """)
        drive_layout.addWidget(self.drive_list)
        
        layout.addWidget(drive_group)
        
        # 按钮
        button_group = QWidget()
        button_layout = QHBoxLayout()
        button_group.setLayout(button_layout)
        
        self.refresh_btn = QPushButton("🔄 手动刷新")
        self.refresh_btn.clicked.connect(self.refresh_drives)
        
        self.find_btn = QPushButton("🔍 查找占用进程")
        self.find_btn.clicked.connect(self.start_find_processes)
        
        self.unlock_eject_btn = QPushButton("🔓 解除占用并弹出")
        self.unlock_eject_btn.clicked.connect(self.start_unlock_and_eject)
        
        button_layout.addWidget(self.refresh_btn)
        button_layout.addWidget(self.find_btn)
        button_layout.addWidget(self.unlock_eject_btn)
        
        layout.addWidget(button_group)
        
        # 进度条
        self.progress_label = QLabel("🟢 准备就绪")
        layout.addWidget(self.progress_label)
        
        self.progress_bar = QProgressBar()
        self.progress_bar.setRange(0, 100)
        self.progress_bar.setTextVisible(True)
        layout.addWidget(self.progress_bar)
        
        # 进程信息 (现在也包含日志信息)
        process_group = QWidget()
        process_layout = QVBoxLayout()
        process_group.setLayout(process_layout)
        
        process_label = QLabel("📊 进程信息与日志")
        process_label.setStyleSheet("font-weight: bold;")
        process_layout.addWidget(process_label)
        
        self.process_text = QTextEdit()
        self.process_text.setReadOnly(True)
        self.process_text.setStyleSheet("""
            QTextEdit {
                font-family: monospace;
                border: 1px solid #c0c0c0;
                border-radius: 4px;
                padding: 2px;
            }
        """)
        process_layout.addWidget(self.process_text)
        
        layout.addWidget(process_group)
        
        # 设置按钮样式
        self.set_button_styles()
        
    def set_button_styles(self):
        """设置按钮自定义样式"""
        button_style = """
            QPushButton {
                padding: 8px;
                border-radius: 4px;
                font-weight: bold;
                border: 1px solid #a0a0a0;
                min-width: 80px;
            }
            QPushButton:hover {
                background-color: #e0e0e0;
            }
            QPushButton:pressed {
                background-color: #d0d0d0;
                border: 1px solid #808080;
            }
            QPushButton:disabled {
                color: #a0a0a0;
                background-color: #f0f0f0;
                border: 1px solid #c0c0c0;
            }
        """
        
        # 为每个按钮设置不同的背景色
        self.refresh_btn.setStyleSheet(button_style + "background-color: #e6f3ff;")
        self.find_btn.setStyleSheet(button_style + "background-color: #fff2cc;")
        self.unlock_eject_btn.setStyleSheet(button_style + "background-color: #e6ffe6;")
        
    def create_system_tray(self):
        """创建系统托盘图标和菜单"""
        self.tray_icon = QSystemTrayIcon(self)
        
        # 设置托盘图标
        if QSystemTrayIcon.isSystemTrayAvailable():
            # 使用系统内置图标
            icon = self.style().standardIcon(QStyle.SP_DriveCDIcon)
            self.tray_icon.setIcon(icon)
        
        # 创建托盘菜单
        self.tray_menu = QMenu()
        
        # 添加显示主窗口选项
        show_action = QAction("显示主窗口", self)
        show_action.triggered.connect(self.show_normal)
        self.tray_menu.addAction(show_action)
        
        # 添加分隔线
        self.tray_menu.addSeparator()
        
        # 添加USB驱动器弹出菜单
        self.usb_menu = QMenu("安全弹出USB驱动器")
        self.tray_menu.addMenu(self.usb_menu)
        
        # 添加分隔线
        self.tray_menu.addSeparator()
        
        # 添加退出选项
        exit_action = QAction("退出", self)
        exit_action.triggered.connect(self.safe_exit)
        self.tray_menu.addAction(exit_action)
        
        # 设置托盘菜单
        self.tray_icon.setContextMenu(self.tray_menu)
        
        # 连接托盘图标点击事件
        self.tray_icon.activated.connect(self.tray_icon_clicked)
        
        # 只有在系统支持托盘图标时才显示
        if QSystemTrayIcon.isSystemTrayAvailable():
            self.tray_icon.show()
            
        # 初始化USB驱动器菜单
        self.update_usb_tray_menu()
        
    def update_usb_tray_menu(self):
        """更新托盘菜单中的USB驱动器列表"""
        self.usb_menu.clear()
        drives = self.get_removable_drives()
        
        if not drives:
            action = QAction("没有可移动驱动器", self)
            action.setEnabled(False)
            self.usb_menu.addAction(action)
            return
            
        for drive in drives:
            volume_name = self.get_volume_name(drive)
            action = QAction(f"{drive} - {volume_name}", self)
            action.setData(drive)  # 存储驱动器字母
            action.triggered.connect(lambda checked, d=drive: self.tray_eject_drive(d))
            self.usb_menu.addAction(action)
        
    def tray_eject_drive(self, drive):
        """从托盘菜单弹出驱动器"""
        if self.running:
            QMessageBox.warning(self, "警告", "已有操作正在进行")
            return
            
        drive_letter = drive[0].upper()
        
        # 创建进度对话框
        self.progress_dialog = EjectProgressDialog(self, drive_letter)
        self.progress_dialog.rejected.connect(self.cancel_eject)
        
        # 显示对话框
        self.progress_dialog.show()
        
        # 开始弹出操作
        self.start_tray_eject(drive_letter)
        
    def start_tray_eject(self, drive_letter):
        """开始从托盘弹出驱动器"""
        self.process_text.clear()
        self.log_message(f"🔓 (托盘操作) 准备解除占用并弹出 {drive_letter}:...")
        
        self.running = True
        
        self.worker_thread = WorkerThread(drive_letter, 'unlock_and_eject')
        self.worker_thread.update_progress.connect(self.update_tray_progress)
        self.worker_thread.update_process_text.connect(self.process_text.append)
        self.worker_thread.operation_complete.connect(self.tray_eject_complete)
        self.worker_thread.show_message.connect(self.show_message)
        self.worker_thread.start()
        
    def update_tray_progress(self, text, value, max_value):
        """更新托盘操作的进度对话框"""
        if self.progress_dialog:
            self.progress_dialog.update_progress(text, value, max_value)
    
    def cancel_eject(self):
        """取消弹出操作"""
        if self.worker_thread and self.worker_thread.isRunning():
            self.worker_thread.stop()
            self.running = False
            if self.progress_dialog:
                self.progress_dialog.close()
            self.log_message("⏹ 用户取消操作")
        
    def tray_eject_complete(self):
        """托盘弹出操作完成"""
        self.running = False
        
        # 关闭进度对话框
        if self.progress_dialog:
            self.progress_dialog.close()
            self.progress_dialog = None
        
        # 更新托盘菜单
        self.update_usb_tray_menu()
        
        # 刷新驱动器列表
        QTimer.singleShot(1000, self.refresh_drives)
        
    def tray_icon_clicked(self, reason):
        """处理托盘图标点击事件"""
        if reason == QSystemTrayIcon.Trigger:  # 单击
            if self.isVisible():
                self.hide()
            else:
                self.show_normal()
        elif reason == QSystemTrayIcon.Context:  # 右键
            self.update_usb_tray_menu()  # 更新USB驱动器菜单
                
    def show_normal(self):
        """正常显示窗口"""
        self.show()
        self.setWindowState(self.windowState() & ~Qt.WindowMinimized | Qt.WindowActive)
        self.activateWindow()
        
    def closeEvent(self, event):
        """重写关闭事件以最小化到托盘"""
        if self.tray_icon.isVisible():
            self.hide()
            event.ignore()
            
    def refresh_drives(self):
        """刷新可移动驱动器列表"""
        self.drive_list.clear()
        drives = self.get_removable_drives()
        for drive in drives:
            volume_name = self.get_volume_name(drive)
            self.drive_list.addItem(f"{drive} - {volume_name}")
        
        # 同时更新托盘菜单
        self.update_usb_tray_menu()
        
    def get_removable_drives(self):
        """获取所有可移动驱动器"""
        drives = []
        bitmask = ctypes.windll.kernel32.GetLogicalDrives()
        for letter in 'ABCDEFGHIJKLMNOPQRSTUVWXYZ':
            if bitmask & 1:
                drive_type = ctypes.windll.kernel32.GetDriveTypeW(f"{letter}:\\")
                if drive_type == win32con.DRIVE_REMOVABLE:
                    drives.append(f"{letter}:")
            bitmask >>= 1
        return drives
    
    def get_volume_name(self, drive):
        """获取驱动器的卷名"""
        try:
            volume_name = win32api.GetVolumeInformation(f"{drive}\\")[0]
            return volume_name if volume_name else "无标签"
        except:
            return "无法访问"
    
    def start_find_processes(self):
        """开始查找锁定进程"""
        if self.running:
            QMessageBox.warning(self, "警告", "已有操作正在进行")
            return
        
        selected_items = self.drive_list.selectedItems()
        if not selected_items:
            QMessageBox.warning(self, "警告", "请先选择一个驱动器")
            return
            
        drive = selected_items[0].text().split()[0]
        drive_letter = drive[0].upper()
        
        self.process_text.clear()
        self.log_message(f"🔍 准备查找锁定 {drive} 的进程...")
        
        self.set_buttons_enabled(False)
        self.running = True
        
        self.worker_thread = WorkerThread(drive_letter, 'find')
        self.worker_thread.update_progress.connect(self.update_progress)
        self.worker_thread.update_process_text.connect(self.process_text.append)
        self.worker_thread.operation_complete.connect(self.operation_complete)
        self.worker_thread.show_message.connect(self.show_message)
        self.worker_thread.start()
    
    def start_unlock_and_eject(self):
        """开始解除占用并弹出驱动器"""
        if self.running:
            QMessageBox.warning(self, "警告", "已有操作正在进行")
            return
        
        selected_items = self.drive_list.selectedItems()
        if not selected_items:
            QMessageBox.warning(self, "警告", "请先选择一个驱动器")
            return
            
        drive = selected_items[0].text().split()[0]
        drive_letter = drive[0].upper()
        
        self.process_text.clear()
        self.log_message(f"🔓 准备解除占用并弹出 {drive}...")
        
        self.set_buttons_enabled(False)
        self.running = True
        
        self.worker_thread = WorkerThread(drive_letter, 'unlock_and_eject')
        self.worker_thread.update_progress.connect(self.update_progress)
        self.worker_thread.update_process_text.connect(self.process_text.append)
        self.worker_thread.operation_complete.connect(self.operation_complete)
        self.worker_thread.show_message.connect(self.show_message)
        self.worker_thread.start()
    
    def get_timestamp(self):
        """获取当前时间戳，格式为[HH:MM:SS]"""
        return datetime.now().strftime("[%H:%M:%S]")
    
    def update_progress(self, text, value=None, max_value=None):
        """更新进度显示"""
        self.progress_label.setText(text)
        if value is not None and max_value is not None:
            self.progress_bar.setMaximum(max_value)
            self.progress_bar.setValue(value)
    
    def log_message(self, message):
        """记录日志消息到进程信息窗口"""
        timestamp = self.get_timestamp()
        self.process_text.append(f"{timestamp} {message}")
    
    def show_message(self, title, message, icon_type):
        """显示消息框"""
        if icon_type == "information":
            QMessageBox.information(self, title, message)
        elif icon_type == "warning":
            QMessageBox.warning(self, title, message)
        elif icon_type == "critical":
            QMessageBox.critical(self, title, message)
        else:
            QMessageBox.information(self, title, message)
    
    def operation_complete(self):
        """处理操作完成"""
        self.running = False
        self.set_buttons_enabled(True)
        self.update_progress("🟢 准备就绪")
        
        # 操作后自动刷新驱动器
        QTimer.singleShot(1000, self.refresh_drives)
    
    def set_buttons_enabled(self, enabled):
        """启用或禁用按钮"""
        self.refresh_btn.setEnabled(enabled)
        self.find_btn.setEnabled(enabled)
        self.unlock_eject_btn.setEnabled(enabled)
    
    def safe_exit(self):
        """安全退出应用程序"""
        if self.running:
            reply = QMessageBox.question(
                self, 
                "确认退出", 
                "有操作正在进行，确定要退出吗？",
                QMessageBox.Yes | QMessageBox.No
            )
            if reply == QMessageBox.No:
                return
        
        self.tray_icon.hide()
        QApplication.quit()

def main():
    # 检查平台
    if sys.platform != "win32":
        print("本程序仅支持Windows系统")
        sys.exit(1)
    
    # 检查管理员权限
    try:
        is_admin = ctypes.windll.shell32.IsUserAnAdmin()
    except:
        is_admin = False
    
    if not is_admin:
        # 尝试以管理员身份重新启动
        ctypes.windll.shell32.ShellExecuteW(None, "runas", sys.executable, " ".join(sys.argv), None, 1)
        sys.exit(0)
    
    # 检查psutil
    try:
        import psutil
    except ImportError:
        print("需要psutil库。请安装: pip install psutil")
        sys.exit(1)
    
    app = QApplication(sys.argv)
    
    # 设置应用程序样式
    app.setStyle('Fusion')
    
    # 设置应用程序字体
    font = QFont()
    font.setFamily('Microsoft YaHei')
    font.setPointSize(9)
    app.setFont(font)
    
    window = USBEjectorPro()
    window.show()
    
    sys.exit(app.exec_())

if __name__ == "__main__":
    main()
                        
#个人使用，搬运源码
#原文作者：Clay_K
#转载请注明出处
#原文链接：https://blog.csdn.net/Clay_K/article/details/148592038