"""
Git 仓库管理工具
功能：选择仓库路径，管理远端地址、拉取、推送、提交
"""

import os
import subprocess
import tkinter as tk
from tkinter import ttk, messagebox, filedialog, simpledialog
from pathlib import Path


class GitRepoManager:
    def __init__(self, root):
        self.root = root
        self.root.title("Git 仓库管理工具")
        self.root.geometry("720x540")
        self.root.resizable(True, True)

        self.repo_path = tk.StringVar()
        self.remote_info = tk.StringVar(value="未选择仓库")

        self._setup_ui()

    # ── UI 搭建 ──────────────────────────────────────────────

    def _setup_ui(self):
        main = ttk.Frame(self.root, padding=12)
        main.pack(fill=tk.BOTH, expand=True)

        # ── 路径选择区 ──
        path_frame = ttk.LabelFrame(main, text="仓库路径", padding=8)
        path_frame.pack(fill=tk.X, pady=(0, 8))

        ttk.Entry(path_frame, textvariable=self.repo_path).pack(
            side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 6))
        ttk.Button(path_frame, text="浏览…", command=self._browse_repo).pack(side=tk.RIGHT)
        ttk.Button(path_frame, text="加载仓库", command=self._load_repo).pack(side=tk.RIGHT, padx=(0, 6))

        # ── 远程信息 ──
        info_frame = ttk.LabelFrame(main, text="远程信息", padding=8)
        info_frame.pack(fill=tk.X, pady=(0, 8))

        self.lbl_remote = ttk.Label(info_frame, textvariable=self.remote_info,
                                    foreground="#555")
        self.lbl_remote.pack(anchor=tk.W)

        # ── 远程管理 ──
        remote_mgr_frame = ttk.LabelFrame(main, text="远程地址管理", padding=8)
        remote_mgr_frame.pack(fill=tk.X, pady=(0, 8))

        btn_row = ttk.Frame(remote_mgr_frame)
        btn_row.pack(fill=tk.X)

        ttk.Button(btn_row, text="查看远程", command=self._list_remotes).pack(
            side=tk.LEFT, padx=2)
        ttk.Button(btn_row, text="添加远程", command=self._add_remote).pack(
            side=tk.LEFT, padx=2)
        ttk.Button(btn_row, text="修改远程", command=self._set_remote_url).pack(
            side=tk.LEFT, padx=2)
        ttk.Button(btn_row, text="删除远程", command=self._remove_remote).pack(
            side=tk.LEFT, padx=2)

        # ── 操作按钮 ──
        action_frame = ttk.LabelFrame(main, text="仓库操作", padding=8)
        action_frame.pack(fill=tk.X, pady=(0, 8))

        btn_row2 = ttk.Frame(action_frame)
        btn_row2.pack(fill=tk.X)

        self.btn_pull = ttk.Button(btn_row2, text="拉取 (Pull)", command=self._git_pull)
        self.btn_pull.pack(side=tk.LEFT, padx=4, ipadx=10)

        self.btn_push = ttk.Button(btn_row2, text="推送 (Push)", command=self._git_push)
        self.btn_push.pack(side=tk.LEFT, padx=4, ipadx=10)

        self.btn_commit = ttk.Button(btn_row2, text="提交 (Commit)", command=self._git_commit)
        self.btn_commit.pack(side=tk.LEFT, padx=4, ipadx=10)

        ttk.Button(btn_row2, text="当前状态", command=self._git_status).pack(
            side=tk.LEFT, padx=4, ipadx=6)

        # ── 输出区 ──
        out_frame = ttk.LabelFrame(main, text="输出日志", padding=8)
        out_frame.pack(fill=tk.BOTH, expand=True)

        self.txt_output = tk.Text(out_frame, wrap=tk.WORD, height=14,
                                  font=("Consolas", 10))
        self.txt_output.config(state=tk.DISABLED)

        scrollbar = ttk.Scrollbar(out_frame, orient=tk.VERTICAL,
                                  command=self.txt_output.yview)
        self.txt_output.configure(yscrollcommand=scrollbar.set)

        self.txt_output.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        # ── 底部状态栏 ──
        self.status_bar = ttk.Label(main, text="就绪", relief=tk.SUNKEN,
                                    anchor=tk.W, padding=(4, 2))
        self.status_bar.pack(fill=tk.X, pady=(4, 0))

    # ── 工具方法 ─────────────────────────────────────────────

    def _log(self, text: str):
        """追加文本到输出区"""
        self.txt_output.config(state=tk.NORMAL)
        self.txt_output.insert(tk.END, text + "\n")
        self.txt_output.see(tk.END)
        self.txt_output.config(state=tk.DISABLED)
        self.root.update_idletasks()

    def _set_status(self, text: str):
        self.status_bar.config(text=text)
        self.root.update_idletasks()

    def _run_git(self, *args: str) -> subprocess.CompletedProcess:
        """执行 git 命令并返回结果"""
        cmd = ["git"] + list(args)
        self._log(f"$ git {' '.join(args)}")
        self._set_status("执行中…")
        try:
            result = subprocess.run(
                cmd,
                cwd=self.repo_path.get(),
                capture_output=True,
                text=True,
                encoding="utf-8",
                timeout=60,
            )
            if result.stdout:
                self._log(result.stdout.rstrip())
            if result.stderr:
                self._log(result.stderr.rstrip())
            if result.returncode != 0:
                self._log(f"⚠ 退出码: {result.returncode}")
            self._set_status("完成" if result.returncode == 0 else "出错")
            return result
        except FileNotFoundError:
            self._log("❌ 未找到 git 命令，请确认已安装 Git")
            self._set_status("错误")
            raise
        except subprocess.TimeoutExpired:
            self._log("⏱ 命令执行超时")
            self._set_status("超时")
            raise

    def _check_repo(self) -> bool:
        """检查是否已选择有效仓库"""
        path = self.repo_path.get()
        if not path:
            messagebox.showwarning("提示", "请先选择仓库路径")
            return False
        git_dir = Path(path) / ".git"
        if not git_dir.is_dir():
            messagebox.showerror("错误", "所选目录不是一个 Git 仓库（没有 .git 目录）")
            return False
        return True

    # ── 事件处理 ─────────────────────────────────────────────

    def _browse_repo(self):
        path = filedialog.askdirectory(title="选择 Git 仓库目录")
        if path:
            self.repo_path.set(path)
            if (Path(path) / ".git").is_dir():
                self._load_repo()
            else:
                self.remote_info.set("⚠ 所选目录不是 Git 仓库")
                self._log("⚠ 所选目录不是 Git 仓库")

    def _load_repo(self):
        if not os.path.isdir(self.repo_path.get()):
            messagebox.showerror("错误", "路径不存在")
            return
        if not (Path(self.repo_path.get()) / ".git").is_dir():
            messagebox.showerror("错误", "不是有效的 Git 仓库")
            return
        self._log(f"📂 已加载仓库: {self.repo_path.get()}")
        self._list_remotes()

    def _list_remotes(self):
        if not self._check_repo():
            return
        try:
            result = self._run_git("remote", "-v")
            if result.returncode != 0:
                return
            remotes = result.stdout.strip()
            if remotes:
                lines = remotes.splitlines()
                # 去重显示
                seen = {}
                for line in lines:
                    parts = line.split()
                    if len(parts) >= 2:
                        name = parts[0]
                        url = parts[1]
                        seen[name] = url
                info_lines = [f"  {k}  →  {v}" for k, v in seen.items()]
                self.remote_info.set("\n".join(info_lines))
                self._log(f"🌐 远程仓库 ({len(seen)} 个):\n" + "\n".join(info_lines))
            else:
                self.remote_info.set("⚠ 没有配置远程仓库")
                self._log("⚠ 没有配置远程仓库")
        except Exception:
            pass

    def _add_remote(self):
        if not self._check_repo():
            return
        name = simpledialog.askstring("添加远程", "远程名称 (如 origin):",
                                      parent=self.root)
        if not name:
            return
        url = simpledialog.askstring("添加远程", f"远程 URL ({name}):",
                                     parent=self.root)
        if not url:
            return
        try:
            result = self._run_git("remote", "add", name, url)
            if result.returncode == 0:
                messagebox.showinfo("成功", f"已添加远程仓库 {name}")
                self._list_remotes()
        except Exception:
            pass

    def _set_remote_url(self):
        if not self._check_repo():
            return
        name = simpledialog.askstring("修改远程", "要修改的远程名称 (如 origin):",
                                      parent=self.root)
        if not name:
            return
        url = simpledialog.askstring("修改远程", f"新的 URL ({name}):",
                                     parent=self.root)
        if not url:
            return
        try:
            result = self._run_git("remote", "set-url", name, url)
            if result.returncode == 0:
                messagebox.showinfo("成功", f"已修改远程 {name} 的 URL")
                self._list_remotes()
        except Exception:
            pass

    def _remove_remote(self):
        if not self._check_repo():
            return
        name = simpledialog.askstring("删除远程", "要删除的远程名称:",
                                      parent=self.root)
        if not name:
            return
        if not messagebox.askyesno("确认", f"确定要删除远程仓库 {name} 吗？"):
            return
        try:
            result = self._run_git("remote", "remove", name)
            if result.returncode == 0:
                messagebox.showinfo("成功", f"已删除远程 {name}")
                self._list_remotes()
        except Exception:
            pass

    def _git_pull(self):
        if not self._check_repo():
            return
        self._log("── 拉取开始 ──")
        try:
            self._run_git("pull", "--all")
        except Exception:
            pass
        self._log("── 拉取结束 ──")

    def _git_push(self):
        if not self._check_repo():
            return
        self._log("── 推送开始 ──")
        try:
            self._run_git("push", "--all")
            self._run_git("push", "--tags")
        except Exception:
            pass
        self._log("── 推送结束 ──")

    def _git_commit(self):
        if not self._check_repo():
            return
        # 暂存所有变更
        self._log("── 提交开始 ──")
        try:
            # 先检查是否有变更
            status_result = self._run_git("status", "--porcelain")
            if status_result.returncode != 0:
                return
            if not status_result.stdout.strip():
                messagebox.showinfo("提示", "没有检测到需要提交的变更")
                self._log("ℹ 没有变更需要提交")
                return

            # 询问提交信息
            msg = simpledialog.askstring("提交", "请输入提交信息:",
                                         parent=self.root)
            if not msg:
                self._log("❌ 提交已取消")
                return

            # add 所有变更
            self._run_git("add", "-A")
            # commit
            result = self._run_git("commit", "-m", msg)
            if result.returncode == 0:
                messagebox.showinfo("成功", "提交成功！\n\n注：仅提交到本地，需点击「推送」才会上传到远程。")
        except Exception:
            pass
        self._log("── 提交结束 ──")

    def _git_status(self):
        if not self._check_repo():
            return
        self._log("── 仓库状态 ──")
        try:
            self._run_git("status")
        except Exception:
            pass
        self._log("── 状态结束 ──")


def main():
    root = tk.Tk()
    app = GitRepoManager(root)
    root.mainloop()


if __name__ == "__main__":
    main()
