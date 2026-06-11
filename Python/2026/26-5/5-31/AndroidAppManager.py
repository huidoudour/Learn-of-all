# Android应用管理工具 (Python版)
# 仅由 huidoudour(慧兜兜)学习使用
# 该程序仅用于学习和研究，不建议在生产环境中使用
import tkinter as tk
from tkinter import ttk, messagebox
import subprocess
import threading
import re
from datetime import datetime

class AndroidAppManager:
    """Android应用管理工具 - 通过ADB管理Android设备和AVD上的应用"""

    # ── 配色方案 ──
    BG_DARK       = "#26282A"
    BG_CARD       = "#34373B"
    BG_HOVER      = "#3E4146"
    TEXT_PRIMARY  = "#BEC3C8"
    TEXT_SECONDARY= "#7D828A"
    BORDER_COLOR  = "#44474C"
    PRIMARY_COLOR = "#388E5A"
    SUCCESS_COLOR = "#388E5A"
    WARNING_COLOR = "#B98C32"
    DANGER_COLOR  = "#AF4B41"
    ACCENT_PURPLE = "#9182A5"

    def __init__(self):
        self.all_apps: list[dict] = []
        self.user_apps_map: dict[int, list[dict]] = {}

        self.root = tk.Tk()
        self.root.title("Android应用管理工具 - 慧兜兜专用版")
        self._setup_ui()
        self._load_devices()

    def log(self, msg: str) -> None:
        ts = datetime.now().strftime("%H:%M:%S")
        print(f"[{ts}] {msg}")

    def log_info(self, msg: str) -> None: self.log(f"INFO: {msg}")
    def log_success(self, msg: str) -> None: self.log(f"SUCCESS: {msg}")
    def log_warning(self, msg: str) -> None: self.log(f"WARNING: {msg}")
    def log_error(self, msg: str) -> None: self.log(f"ERROR: {msg}")

    # ═══════════════════════════════════════════════════════
    #  UI构建
    # ═══════════════════════════════════════════════════════

    def _setup_ui(self):
        self.root.geometry("1280x720")
        self.root.minsize(1000, 600)
        self.root.configure(bg=self.BG_DARK)

        # 自定义标题栏
        self._create_title_bar()

        # 主面板
        main = tk.Frame(self.root, bg=self.BG_DARK)
        main.pack(fill=tk.BOTH, expand=True, padx=12, pady=(0, 12))

        # 顶部控制面板
        self._create_top_panel(main)

        # 中间双栏
        self._create_center_panel(main)

        # 状态栏
        self.status_var = tk.StringVar(value="就绪 | 提示：右键或双击应用进行操作")
        status_bar = tk.Label(main, textvariable=self.status_var,
                              bg=self.BG_DARK, fg=self.TEXT_SECONDARY,
                              font=("Microsoft YaHei", 10), anchor=tk.W)
        status_bar.pack(fill=tk.X, pady=(8, 0))

    def _create_title_bar(self):
        """自定义深色标题栏"""
        bar = tk.Frame(self.root, bg="#1E2022", height=34)
        bar.pack(fill=tk.X)
        bar.pack_propagate(False)

        title = tk.Label(bar, text="  Android应用管理工具 - 慧兜兜专用版",
                         bg="#1E2022", fg="#AAAFB6",
                         font=("Microsoft YaHei", 10, "bold"))
        title.pack(side=tk.LEFT, padx=8)

        btn_frame = tk.Frame(bar, bg="#1E2022")
        btn_frame.pack(side=tk.RIGHT)

        def make_tb_btn(text, hover_color=None, click_cmd=None):
            btn = tk.Label(btn_frame, text=text, bg="#1E2022", fg="#BEC3C8",
                           font=("Microsoft YaHei", 12), width=3, cursor="hand2")
            btn.pack(side=tk.LEFT, padx=1)
            if hover_color:
                btn.bind("<Enter>", lambda e, b=btn, c=hover_color: b.configure(bg=c))
                btn.bind("<Leave>", lambda e, b=btn: b.configure(bg="#1E2022"))
            if click_cmd:
                btn.bind("<Button-1>", lambda e, cmd=click_cmd: cmd())
            return btn

        close_btn = make_tb_btn("✕", self.DANGER_COLOR, self.root.destroy)

        def toggle_max():
            self.root.attributes('-fullscreen',
                                not self.root.attributes('-fullscreen'))
        max_btn = make_tb_btn("□", self.BG_HOVER, toggle_max)

        min_btn = make_tb_btn("—", self.BG_HOVER, self.root.iconify)

        # 拖拽窗口
        bar.bind("<Button-1>", self._start_drag)
        bar.bind("<B1-Motion>", self._do_drag)
        title.bind("<Button-1>", self._start_drag)
        title.bind("<B1-Motion>", self._do_drag)

    def _start_drag(self, event):
        self._drag_x = event.x
        self._drag_y = event.y

    def _do_drag(self, event):
        self.root.geometry(f"+{event.x_root - self._drag_x}+{event.y_root - self._drag_y}")

    def _create_top_panel(self, parent):
        panel = tk.Frame(parent, bg=self.BG_CARD, highlightbackground=self.BORDER_COLOR,
                         highlightthickness=1)
        panel.pack(fill=tk.X, pady=(0, 10))

        inner = tk.Frame(panel, bg=self.BG_CARD)
        inner.pack(fill=tk.X, padx=14, pady=10)

        tk.Label(inner, text="选择设备:", bg=self.BG_CARD, fg=self.TEXT_PRIMARY,
                 font=("Microsoft YaHei", 10, "bold")).pack(side=tk.LEFT)

        self.device_var = tk.StringVar()
        self.device_combo = ttk.Combobox(inner, textvariable=self.device_var,
                                         state="readonly", width=36, font=("Microsoft YaHei", 10))
        self.device_combo.pack(side=tk.LEFT, padx=(6, 12))

        self._styled_button(inner, "刷新设备", self.PRIMARY_COLOR,
                            self._load_devices).pack(side=tk.LEFT, padx=4)
        self._styled_button(inner, "加载应用", self.SUCCESS_COLOR,
                            self._load_apps).pack(side=tk.LEFT, padx=4)

    def _create_center_panel(self, parent):
        """左右双栏布局"""
        center = tk.Frame(parent, bg=self.BG_DARK)
        center.pack(fill=tk.BOTH, expand=True)

        center.grid_columnconfigure(0, weight=1)
        center.grid_columnconfigure(1, weight=1)
        center.grid_rowconfigure(0, weight=1)

        left = self._create_user_panel(center, "用户 0 (主用户)", self.PRIMARY_COLOR)
        left.grid(row=0, column=0, sticky="nsew", padx=(0, 5))

        right = self._create_user_panel(center, "其他用户", self.ACCENT_PURPLE)
        right.grid(row=0, column=1, sticky="nsew", padx=(5, 0))

        # 保存引用
        self.left_panel_ref = left
        self.right_panel_ref = right

    def _create_user_panel(self, parent, title_text, accent_color):
        """创建单个用户面板"""
        frame = tk.Frame(parent, bg=self.BG_CARD,
                         highlightbackground=self.BORDER_COLOR, highlightthickness=1)
        frame.grid_propagate(False)

        # 标题
        header = tk.Frame(frame, bg=self.BG_CARD)
        header.pack(fill=tk.X, padx=10, pady=(8, 4))
        tk.Label(header, text=title_text, bg=self.BG_CARD, fg=accent_color,
                 font=("Microsoft YaHei", 11, "bold")).pack(side=tk.LEFT)

        # 表格 (Treeview)
        columns = ("package", "type")
        tree = ttk.Treeview(frame, columns=columns, show="headings",
                            selectmode="browse")
        tree.heading("package", text="包名")
        tree.heading("type", text="类型")
        tree.column("package", width=260, minwidth=120)
        tree.column("type", width=100, minwidth=60)

        # 深色主题样式
        style = ttk.Style()
        style.theme_use("clam")
        style.configure("Treeview",
                        background=self.BG_CARD, foreground=self.TEXT_PRIMARY,
                        fieldbackground=self.BG_CARD, borderwidth=0,
                        rowheight=26, font=("Microsoft YaHei", 9))
        style.configure("Treeview.Heading",
                        background="#374440" if "主用户" in title_text else "#443F4A",
                        foreground=self.TEXT_PRIMARY,
                        font=("Microsoft YaHei", 10, "bold"),
                        borderwidth=0)
        style.map("Treeview",
                  background=[("selected", "#3A4350")],
                  foreground=[("selected", self.TEXT_PRIMARY)])
        style.map("Treeview.Heading",
                  background=[("active",
                               "#374440" if "主用户" in title_text else "#443F4A")])

        tree.pack(fill=tk.BOTH, expand=True, padx=8, pady=(0, 4))

        # 右键菜单
        self._add_context_menu(tree, "主用户" in title_text)

        # 双击事件
        tree.bind("<Double-1>", lambda e, t=tree, u="主用户" in title_text:
                  self._show_app_operations(t, u))

        # 提示
        hint = tk.Label(frame, text="右键或双击应用进行操作",
                        bg=self.BG_CARD, fg=self.TEXT_SECONDARY,
                        font=("Microsoft YaHei", 9))
        hint.pack(pady=(0, 6))

        return frame

    def _add_context_menu(self, tree, is_user0):
        menu = tk.Menu(tree, tearoff=0, bg=self.BG_CARD, fg=self.TEXT_PRIMARY,
                       activebackground=self.BG_HOVER, activeforeground=self.TEXT_PRIMARY,
                       font=("Microsoft YaHei", 10))

        menu.add_command(label="启动应用", command=lambda: self._do_action(tree, is_user0, "launch"))
        menu.add_separator()
        menu.add_command(label="强制停止", command=lambda: self._do_action(tree, is_user0, "force_stop"))
        menu.add_command(label="杀死进程", command=lambda: self._do_action(tree, is_user0, "kill"))
        menu.add_separator()
        menu.add_command(label="卸载应用", command=lambda: self._do_action(tree, is_user0, "uninstall"))
        menu.add_command(label="清除数据", command=lambda: self._do_action(tree, is_user0, "clear_data"))
        menu.add_command(label="清除缓存", command=lambda: self._do_action(tree, is_user0, "clear_cache"))
        menu.add_separator()
        menu.add_command(label="复制包名", command=lambda: self._do_action(tree, is_user0, "copy_package"))

        def popup(event):
            if tree.identify_row(event.y):
                tree.selection_set(tree.identify_row(event.y))
                menu.post(event.x_root, event.y_root)

        tree.bind("<Button-3>", popup)

    # ═══════════════════════════════════════════════════════
    #  辅助UI方法
    # ═══════════════════════════════════════════════════════

    def _styled_button(self, parent, text, color, command):
        btn = tk.Button(parent, text=text, bg=color, fg="#E4E6EB",
                        font=("Microsoft YaHei", 10, "bold"),
                        relief=tk.FLAT, cursor="hand2",
                        activebackground=self._lighten(color),
                        activeforeground="#E4E6EB",
                        padx=16, pady=4, command=command)
        btn.bind("<Enter>", lambda e, b=btn, c=color: b.configure(bg=self._lighten(c)))
        btn.bind("<Leave>", lambda e, b=btn, c=color: b.configure(bg=c))
        return btn

    @staticmethod
    def _lighten(hex_color: str) -> str:
        r, g, b = int(hex_color[1:3], 16), int(hex_color[3:5], 16), int(hex_color[5:7], 16)
        r = min(255, r + 20)
        g = min(255, g + 20)
        b = min(255, b + 20)
        return f"#{r:02x}{g:02x}{b:02x}"

    def _set_status(self, text: str):
        self.root.after(0, lambda: self.status_var.set(text))

    def _get_selected_app(self, tree, is_user0) -> dict | None:
        sel = tree.selection()
        if not sel:
            messagebox.showwarning("提示", "请先选择一个应用")
            return None
        values = tree.item(sel[0], "values")
        if not values:
            return None
        pkg = values[0]
        if is_user0:
            apps = self.user_apps_map.get(0, [])
        else:
            apps = self._get_all_other_apps()
        idx = tree.index(sel[0])
        if 0 <= idx < len(apps):
            return apps[idx]
        return None

    def _get_all_other_apps(self) -> list[dict]:
        result = []
        for uid, apps in self.user_apps_map.items():
            if uid != 0:
                result.extend(apps)
        return result

    # ═══════════════════════════════════════════════════════
    #  设备与应用加载
    # ═══════════════════════════════════════════════════════

    def _load_devices(self):
        self.log_info("开始检测设备...")
        self._set_status("正在检测设备...")

        def run():
            try:
                proc = subprocess.run(["adb", "devices"], capture_output=True, text=True)
                devices = []
                for line in proc.stdout.strip().split("\n")[1:]:
                    parts = line.strip().split()
                    if len(parts) >= 2 and parts[1] == "device":
                        devices.append(parts[0])
                        self.log_info(f"发现设备: {parts[0]}")

                if not devices:
                    devices = ["未检测到设备"]
                    self.log_warning("未检测到任何设备")
                    self._set_status("未检测到已连接的设备")
                else:
                    self._set_status(f"检测到 {len(devices)} 个设备")
                    self.log_success(f"共检测到 {len(devices)} 个设备")

                self.root.after(0, lambda: self._update_device_combo(devices))
            except FileNotFoundError:
                self.log_error("未找到ADB，请确认已安装Android SDK Platform Tools")
                self.root.after(0, lambda: self._update_device_combo(["ADB未安装"]))
            except Exception as e:
                self.log_error(f"检测设备失败: {e}")
                self.root.after(0, lambda: self._update_device_combo(["检测失败"]))

        threading.Thread(target=run, daemon=True).start()

    def _update_device_combo(self, devices):
        self.device_combo["values"] = devices
        if devices:
            self.device_combo.current(0)

    def _load_apps(self):
        device = self.device_var.get()
        if not device or "未检测到" in device or "未安装" in device or "失败" in device:
            self.log_warning("尝试加载应用但未选择有效设备")
            messagebox.showwarning("警告", "请先选择有效的设备")
            return

        self.log_info(f"开始加载设备 [{device}] 的应用列表...")
        self._set_status("正在加载应用列表...")
        self.all_apps.clear()
        self.user_apps_map.clear()

        def run():
            try:
                user_ids = self._get_user_ids(device)
                self.log_info(f"发现 {len(user_ids)} 个用户: {user_ids}")

                total = 0
                for uid in user_ids:
                    apps = self._get_third_party_apps(device, uid)
                    self.user_apps_map[uid] = apps
                    self.all_apps.extend(apps)
                    total += len(apps)
                    self.log_info(f"用户 {uid} 有 {len(apps)} 个第三方应用")

                self.root.after(0, self._update_tables)
                self._set_status(f"已加载 {len(self.all_apps)} 个应用（{len(user_ids)} 个用户）")
                self.log_success(f"共加载 {total} 个应用")
            except Exception as e:
                self.log_error(f"加载应用列表失败: {e}")
                self.root.after(0, lambda: messagebox.showerror("错误", f"加载应用列表失败: {e}"))

        threading.Thread(target=run, daemon=True).start()

    def _get_user_ids(self, device: str) -> list[int]:
        try:
            proc = subprocess.run(["adb", "-s", device, "shell", "pm", "list", "users"],
                                  capture_output=True, text=True)
            ids = []
            for line in proc.stdout.split("\n"):
                if "UserInfo" in line:
                    m = re.search(r'UserInfo\{(\d+):', line)
                    if m:
                        ids.append(int(m.group(1)))
            return ids if ids else [0]
        except Exception:
            return [0]

    def _get_third_party_apps(self, device: str, user_id: int) -> list[dict]:
        try:
            proc = subprocess.run(
                ["adb", "-s", device, "shell", "pm", "list", "packages", "-3",
                 "--user", str(user_id)],
                capture_output=True, text=True)
            apps = []
            for line in proc.stdout.split("\n"):
                if line.startswith("package:"):
                    pkg = line[8:].strip()
                    apps.append({
                        "packageName": pkg,
                        "userId": user_id,
                        "isHuidoudour": "huidou" in pkg
                    })
            return apps
        except Exception:
            return []

    def _update_tables(self):
        """刷新左右表格"""
        # 找到两个 Treeview
        left_tree = self._find_treeview(self.left_panel_ref)
        right_tree = self._find_treeview(self.right_panel_ref)

        # 清空
        for tree in [left_tree, right_tree]:
            if tree:
                for item in tree.get_children():
                    tree.delete(item)

        # 左栏：用户0
        user0_apps = self.user_apps_map.get(0, [])
        for app in user0_apps:
            ptype = "# huidou" if app["isHuidoudour"] else "第三方"
            if left_tree:
                left_tree.insert("", tk.END, values=(app["packageName"], ptype))

        # 右栏：其他用户
        for uid, apps in self.user_apps_map.items():
            if uid != 0:
                for app in apps:
                    ptype = "# huidou" if app["isHuidoudour"] else "第三方"
                    if right_tree:
                        right_tree.insert("", tk.END,
                                          values=(f"{app['packageName']} [用户{uid}]", ptype))

    def _find_treeview(self, parent):
        """递归查找Frame中的Treeview"""
        for child in parent.winfo_children():
            if isinstance(child, ttk.Treeview):
                return child
            if isinstance(child, tk.Frame):
                result = self._find_treeview(child)
                if result:
                    return result
        return None

    # ═══════════════════════════════════════════════════════
    #  应用操作
    # ═══════════════════════════════════════════════════════

    def _do_action(self, tree, is_user0, action):
        app = self._get_selected_app(tree, is_user0)
        if not app:
            return
        actions = {
            "launch": self._launch_app,
            "force_stop": self._force_stop_app,
            "kill": self._kill_app,
            "uninstall": self._uninstall_app,
            "clear_data": self._clear_data,
            "clear_cache": self._clear_cache,
            "copy_package": self._copy_package,
        }
        if action in actions:
            actions[action](app)

    def _show_app_operations(self, tree, is_user0):
        app = self._get_selected_app(tree, is_user0)
        if not app:
            return

        dialog = tk.Toplevel(self.root)
        dialog.title(f"选择对 \"{app['packageName']}\" 的操作")
        dialog.geometry("280x380")
        dialog.configure(bg=self.BG_CARD)
        dialog.transient(self.root)
        dialog.grab_set()
        dialog.resizable(False, False)

        ops = [
            ("启动应用", self.SUCCESS_COLOR, lambda: (self._launch_app(app), dialog.destroy())),
            ("强制停止", self.WARNING_COLOR, lambda: (self._force_stop_app(app), dialog.destroy())),
            ("杀死进程", self.DANGER_COLOR, lambda: (self._kill_app(app), dialog.destroy())),
            ("卸载应用", self.DANGER_COLOR, lambda: (self._uninstall_app(app), dialog.destroy())),
            ("清除数据", self.WARNING_COLOR, lambda: (self._clear_data(app), dialog.destroy())),
            ("清除缓存", self.PRIMARY_COLOR, lambda: (self._clear_cache(app), dialog.destroy())),
        ]

        for text, color, cmd in ops:
            self._styled_button(dialog, text, color, cmd).pack(fill=tk.X, padx=14, pady=5)

    def _get_device(self) -> str | None:
        device = self.device_var.get()
        if not device or "未检测到" in device or "未安装" in device or "失败" in device:
            self.log_warning("未选择有效设备")
            messagebox.showwarning("警告", "请先选择有效的设备")
            return None
        return device

    def _launch_app(self, app):
        device = self._get_device()
        if not device:
            return
        self._set_status("正在启动应用...")
        self.log_info(f"========== 开始启动应用: {app['packageName']} ==========")

        def run():
            success = self._launch_with_am_start(device, app)
            if not success:
                self.log_warning("【方法1】am start 启动失败，尝试降级方案...")
                self.log_info("【方法2】尝试使用 monkey 启动应用...")
                success = self._launch_with_monkey(device, app)
            if success:
                self.log_success(f"应用启动成功 ({app['packageName']})")
                self.root.after(0, lambda: messagebox.showinfo("成功", "应用启动成功"))
            else:
                self.log_error("所有启动方法均失败")
                self.root.after(0, lambda: messagebox.showerror("失败", "应用启动失败，请检查应用是否正确安装且有LAUNCHER Activity"))
            self.log_info("========== 启动流程结束 ==========")

        threading.Thread(target=run, daemon=True).start()

    def _launch_with_am_start(self, device, app) -> bool:
        try:
            activity = self._get_launch_activity(device, app["packageName"])
            if activity and "/" in activity:
                self.log_info(f"找到启动Activity: {activity}")
                cmd = ["adb", "-s", device, "shell", "am", "start", "-n", activity]
            else:
                self.log_warning("无法获取启动Activity，尝试包名直接启动")
                cmd = ["adb", "-s", device, "shell", "am", "start",
                       "-a", "android.intent.action.MAIN",
                       "-c", "android.intent.category.LAUNCHER",
                       "-p", app["packageName"]]
            return self._exec_adb(cmd)
        except Exception as e:
            self.log_error(f"am start 启动异常: {e}")
            return False

    def _get_launch_activity(self, device, package):
        try:
            proc = subprocess.run(
                ["adb", "-s", device, "shell", "cmd", "package", "resolve-activity",
                 "--brief", package],
                capture_output=True, text=True)
            result = proc.stdout.strip()
            self.log_info(f"Activity查询结果: {result}")
            return result if "/" in result and "No activity" not in result else None
        except Exception as e:
            self.log_warning(f"查询Activity失败: {e}")
            return None

    def _launch_with_monkey(self, device, app) -> bool:
        cmd = ["adb", "-s", device, "shell", "monkey",
               "-p", app["packageName"],
               "-c", "android.intent.category.LAUNCHER", "1"]
        try:
            proc = subprocess.run(cmd, capture_output=True, text=True)
            output = proc.stdout + proc.stderr
            self.log_info(f"monkey ADB输出: {output.strip()}")
            return "Events injected" in output or ":Dropped" in output
        except Exception as e:
            self.log_error(f"monkey 启动异常: {e}")
            return False

    def _force_stop_app(self, app):
        device = self._get_device()
        if not device:
            return
        if not messagebox.askyesno("确认强制停止",
                                   f"确定要强制停止用户{app['userId']}的应用 \"{app['packageName']}\" 吗？\n\n"
                                   "这将立即停止应用的所有活动，可能导致数据丢失。"):
            return

        self._set_status("正在强制停止应用...")
        cmd = ["adb", "-s", device, "shell", "am", "force-stop", app["packageName"]]

        def run():
            if self._exec_adb(cmd):
                self._set_status("操作成功")
                self.log_success(f"应用已强制停止 ({app['packageName']})")
                self.root.after(0, lambda: messagebox.showinfo("成功", "应用已强制停止"))
            else:
                self.root.after(0, lambda: messagebox.showerror("失败", "强制停止应用失败"))

        threading.Thread(target=run, daemon=True).start()

    def _kill_app(self, app):
        device = self._get_device()
        if not device:
            return
        if not messagebox.askyesno("确认杀死进程",
                                   f"警告：此操作较为激进！\n\n"
                                   f"确定要杀死用户{app['userId']}的应用 \"{app['packageName']}\" 的所有进程吗？\n"
                                   "这会比强制停止更彻底，但可能导致系统不稳定。"):
            return

        self._set_status("正在杀死应用进程...")
        cmd = ["adb", "-s", device, "shell", "pkill", "-f", app["packageName"]]

        def run():
            if self._exec_adb(cmd):
                self._set_status("操作成功")
                self.log_success(f"应用进程已杀死 ({app['packageName']})")
                self.root.after(0, lambda: messagebox.showinfo("成功", "应用进程已杀死"))
            else:
                self.root.after(0, lambda: messagebox.showerror("失败", "杀死应用进程失败"))

        threading.Thread(target=run, daemon=True).start()

    def _uninstall_app(self, app):
        device = self._get_device()
        if not device:
            return

        if app["userId"] == 0:
            confirm_msg = ("这是主用户（用户0）的应用\n\n"
                           f"确定要为【所有用户】卸载应用 \"{app['packageName']}\" 吗？\n"
                           "此操作将从所有用户空间中删除该应用，不可恢复！")
            cmd = ["adb", "-s", device, "uninstall", app["packageName"]]
        else:
            confirm_msg = (f"这是用户 {app['userId']} 的应用\n\n"
                           f"确定要仅为【用户{app['userId']}】卸载应用 \"{app['packageName']}\" 吗？\n"
                           "其他用户的应用将不受影响。")
            cmd = ["adb", "-s", device, "shell", "pm", "uninstall",
                   "--user", str(app["userId"]), app["packageName"]]

        if not messagebox.askyesno("确认卸载", confirm_msg):
            return

        self._set_status("正在卸载应用...")
        self.log_info(f"执行命令: {' '.join(cmd)}")

        def run():
            try:
                proc = subprocess.run(cmd, capture_output=True, text=True)
                result = proc.stdout + proc.stderr
                self.log_info(f"ADB输出: {result.strip()}")
                if "Success" in result:
                    msg = "应用已从所有用户中卸载成功" if app["userId"] == 0 else f"应用已从用户{app['userId']}中卸载成功"
                    self.log_success(f"{msg} ({app['packageName']})")
                    self.root.after(0, lambda: messagebox.showinfo("成功", msg))
                    self.root.after(0, self._load_apps)
                else:
                    self.log_error(f"卸载失败: {result}")
                    self.root.after(0, lambda: messagebox.showerror("失败", f"卸载失败: {result}"))
            except Exception as e:
                self.log_error(f"卸载应用异常: {e}")
                self.root.after(0, lambda: messagebox.showerror("失败", f"卸载应用失败: {e}"))

        threading.Thread(target=run, daemon=True).start()

    def _clear_data(self, app):
        device = self._get_device()
        if not device:
            return
        if not messagebox.askyesno("确认清除数据",
                                   f"警告：此操作不可恢复！\n\n"
                                   f"确定要清除用户{app['userId']}的应用 \"{app['packageName']}\" 的所有数据吗？\n"
                                   "包括：登录信息、设置、本地文件等"):
            return

        self._set_status("正在清除应用数据...")
        cmd = ["adb", "-s", device, "shell", "pm", "clear",
               "--user", str(app["userId"]), app["packageName"]]

        def run():
            try:
                proc = subprocess.run(cmd, capture_output=True, text=True)
                result = (proc.stdout + proc.stderr).strip()
                self.log_info(f"ADB输出: {result}")
                if "Success" in result or "success" in result:
                    msg = f"用户{app['userId']}的应用数据清除成功"
                    self.log_success(f"{msg} ({app['packageName']})")
                    self.root.after(0, lambda: messagebox.showinfo("成功", msg))
                else:
                    self.root.after(0, lambda: messagebox.showerror("失败", f"清除数据失败: {result}"))
            except Exception as e:
                self.log_error(f"清除应用数据异常: {e}")
                self.root.after(0, lambda: messagebox.showerror("失败", f"清除应用数据失败: {e}"))

        threading.Thread(target=run, daemon=True).start()

    def _clear_cache(self, app):
        device = self._get_device()
        if not device:
            return
        if not messagebox.askyesno("确认清除缓存",
                                   f"确定要清除用户{app['userId']}的应用 \"{app['packageName']}\" 的缓存吗？\n\n"
                                   "注意：这将只清除缓存文件，不会删除应用数据"):
            return

        self._set_status("正在清除应用缓存...")
        cmd = ["adb", "-s", device, "shell", "pm", "trim-caches", "999G"]

        def run():
            try:
                subprocess.run(cmd, capture_output=True, text=True)
                msg = f"用户{app['userId']}的应用缓存已清除"
                self.log_success(f"{msg} ({app['packageName']})")
                self.root.after(0, lambda: messagebox.showinfo("成功",
                    msg + "\n注意：系统清除了所有应用的缓存以释放空间"))
            except Exception as e:
                self.log_error(f"清除应用缓存异常: {e}")
                self.root.after(0, lambda: messagebox.showerror("失败", f"清除应用缓存失败: {e}"))

        threading.Thread(target=run, daemon=True).start()

    def _copy_package(self, app):
        self.root.clipboard_clear()
        self.root.clipboard_append(app["packageName"])
        msg = f"已复制包名: {app['packageName']}"
        self.log_info(msg)
        self._set_status(msg)

    def _exec_adb(self, cmd: list[str]) -> bool:
        """执行ADB命令并判断是否成功"""
        try:
            self.log_info(f"执行命令: {' '.join(cmd)}")
            proc = subprocess.run(cmd, capture_output=True, text=True)
            result = proc.stdout + proc.stderr
            self.log_info(f"ADB输出: {result.strip()}")
            if "Error" in result or "Exception" in result:
                self.log_warning("命令可能执行失败")
                return False
            return True
        except Exception as e:
            self.log_error(f"命令执行异常: {e}")
            return False

    # ═══════════════════════════════════════════════════════
    #  入口
    # ═══════════════════════════════════════════════════════

    def run(self):
        print("=" * 59)
        print("       Android应用管理工具 v3.0 (Python) - 慧兜兜专用版")
        print("       仅用于学习和研究，不建议在生产环境中使用")
        print("=" * 59)
        self.root.mainloop()


if __name__ == "__main__":
    AndroidAppManager().run()
