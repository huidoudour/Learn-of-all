import os
import subprocess
import tkinter as tk
from tkinter import ttk, messagebox
import threading

class NTFSIndexSearcher:
    def __init__(self, root):
        self.root = root
        self.root.title("NTFS索引搜索工具")
        self.root.geometry("800x600")
        self.root.minsize(600, 400)
        
        # 设置中文字体支持
        self.style = ttk.Style()
        self.style.configure("TButton", font=('SimHei', 10))
        self.style.configure("TLabel", font=('SimHei', 10))
        self.style.configure("TEntry", font=('SimHei', 10))
        
        # 创建界面
        self.create_widgets()
        
        # 搜索线程控制
        self.search_thread = None
        self.stop_search = False
    
    def create_widgets(self):
        # 顶部搜索区域
        search_frame = ttk.Frame(self.root, padding="10")
        search_frame.pack(fill=tk.X)
        
        ttk.Label(search_frame, text="搜索内容：").pack(side=tk.LEFT, padx=(0, 5))
        
        self.search_entry = ttk.Entry(search_frame)
        self.search_entry.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 5))
        self.search_entry.bind('<Return>', lambda event: self.search())
        
        # 搜索和取消按钮
        button_frame = ttk.Frame(search_frame)
        button_frame.pack(side=tk.LEFT)
        
        self.search_button = ttk.Button(button_frame, text="搜索", command=self.search)
        self.search_button.pack(side=tk.LEFT, padx=(0, 5))
        
        self.cancel_button = ttk.Button(button_frame, text="取消", command=self.cancel_search, state=tk.DISABLED)
        self.cancel_button.pack(side=tk.LEFT)
        
        # 结果显示区域
        result_frame = ttk.LabelFrame(self.root, text="搜索结果", padding="10")
        result_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=(0, 10))
        
        # 创建Treeview来显示结果
        columns = ("name", "path", "open", "open_folder")
        self.result_tree = ttk.Treeview(result_frame, columns=columns, show="headings")
        
        # 设置列宽和标题
        self.result_tree.heading("name", text="文件名")
        self.result_tree.heading("path", text="文件路径")
        self.result_tree.heading("open", text="打开文件")
        self.result_tree.heading("open_folder", text="打开文件夹")
        
        self.result_tree.column("name", width=150, anchor=tk.W)
        self.result_tree.column("path", width=400, anchor=tk.W)
        self.result_tree.column("open", width=80, anchor=tk.CENTER)
        self.result_tree.column("open_folder", width=100, anchor=tk.CENTER)
        
        # 添加垂直滚动条
        scrollbar = ttk.Scrollbar(result_frame, orient=tk.VERTICAL, command=self.result_tree.yview)
        self.result_tree.configure(yscroll=scrollbar.set)
        
        # 放置Treeview和滚动条
        self.result_tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        
        # 状态栏
        self.status_var = tk.StringVar()
        self.status_var.set("就绪")
        status_bar = ttk.Label(self.root, textvariable=self.status_var, relief=tk.SUNKEN, anchor=tk.W)
        status_bar.pack(side=tk.BOTTOM, fill=tk.X)
        
        # 绑定双击事件
        self.result_tree.bind('<Double-1>', self.on_item_double_click)
    
    def search(self):
        # 检查是否已有搜索线程在运行
        if self.search_thread and self.search_thread.is_alive():
            messagebox.showinfo("提示", "搜索正在进行中，请等待或点击取消")
            return
        
        # 清空之前的搜索结果
        for item in self.result_tree.get_children():
            self.result_tree.delete(item)
        
        search_text = self.search_entry.get().strip()
        if not search_text:
            messagebox.showwarning("警告", "请输入搜索内容")
            return
        
        # 更新界面状态
        self.status_var.set(f"正在搜索: {search_text}...")
        self.search_button.config(state=tk.DISABLED)
        self.cancel_button.config(state=tk.NORMAL)
        self.stop_search = False
        
        # 在新线程中执行搜索
        self.search_thread = threading.Thread(target=self._search_thread, args=(search_text,))
        self.search_thread.daemon = True
        self.search_thread.start()
    
    def _search_thread(self, search_text):
        try:
            # 尝试使用Windows搜索命令行工具
            results = self.windows_search(search_text)
            
            # 如果Windows搜索失败，尝试使用备选方法
            if not results:
                self.root.after(0, lambda: self.status_var.set("Windows搜索不可用，尝试使用文件系统搜索..."))
                results = self.file_system_search(search_text)
            
            # 在主线程中更新结果
            self.root.after(0, lambda: self.display_results(results))
            
        except Exception as e:
            self.root.after(0, lambda: messagebox.showerror("错误", f"搜索过程中发生错误:\n{str(e)}"))
            self.root.after(0, lambda: self.status_var.set("搜索失败"))
        finally:
            # 恢复界面状态
            self.root.after(0, lambda: self.search_button.config(state=tk.NORMAL))
            self.root.after(0, lambda: self.cancel_button.config(state=tk.DISABLED))
    
    def windows_search(self, search_text):
        """使用Windows命令行搜索工具"""
        try:
            # 转义搜索文本中的特殊字符
            search_text_escaped = search_text.replace('"', '""')
            
            # 使用Windows内置的where命令（只能搜索系统PATH中的文件）
            # 这只是一个简单的示例，实际使用中可能需要更复杂的命令
            cmd = f'powershell -Command "Get-ChildItem -Path C:\, D:\, E:\ -Recurse -Filter \"*{search_text_escaped}*\" -ErrorAction SilentlyContinue | Select-Object FullName"'
            
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=60)
            
            if result.returncode != 0:
                return []
            
            # 解析结果
            results = []
            for line in result.stdout.strip().split('\n'):
                line = line.strip()
                if line and line != "FullName" and os.path.isfile(line):
                    filename = os.path.basename(line)
                    results.append((filename, line))
            
            return results[:1000]  # 限制结果数量以避免过多数据
        except:
            return []
    
    def file_system_search(self, search_text):
        """使用Python的os.walk进行文件系统搜索"""
        results = []
        drives = [f"{d}:\\" for d in 'CDEFGHIJKLMNOPQRSTUVWXYZ' if os.path.exists(f"{d}:\\")]
        
        for drive in drives:
            for root, dirs, files in os.walk(drive):
                if self.stop_search:
                    break
                
                for file in files:
                    if search_text.lower() in file.lower():
                        full_path = os.path.join(root, file)
                        results.append((file, full_path))
                        
                        # 每找到100个结果就更新一次界面
                        if len(results) % 100 == 0:
                            self.root.after(0, lambda r=results.copy():
                                           self.status_var.set(f"已找到 {len(r)} 个结果..."))
                        
                        # 限制结果数量
                        if len(results) >= 1000:
                            return results
            
            if self.stop_search:
                break
        
        return results
    
    def display_results(self, results):
        """在Treeview中显示搜索结果"""
        # 清空现有结果
        for item in self.result_tree.get_children():
            self.result_tree.delete(item)
        
        # 添加新结果
        for name, path in results:
            self.result_tree.insert("", tk.END, values=(name, path, "打开", "打开文件夹"))
        
        self.status_var.set(f"找到 {len(results)} 个结果")
    
    def on_item_double_click(self, event):
        """处理Treeview项目的双击事件"""
        # 获取双击的列和项目
        region = self.result_tree.identify_region(event.x, event.y)
        if region != "cell":
            return
        
        column = self.result_tree.identify_column(event.x)
        item = self.result_tree.identify_row(event.y)
        
        if not item:
            return
        
        # 获取文件路径
        path = self.result_tree.item(item, "values")[1]
        
        # 处理不同列的点击
        if column == "#3":  # 打开文件列
            self.open_file(path)
        elif column == "#4":  # 打开文件夹列
            self.open_folder(path)
    
    def cancel_search(self):
        """取消正在进行的搜索"""
        self.stop_search = True
        self.status_var.set("正在取消搜索...")
    
    def open_file(self, file_path):
        """打开选中的文件"""
        try:
            # 使用Windows默认程序打开文件
            os.startfile(file_path)
        except Exception as e:
            messagebox.showerror("错误", f"无法打开文件:\n{str(e)}")
    
    def open_folder(self, file_path):
        """打开文件所在的文件夹并选中文件"""
        try:
            # 获取文件夹路径
            folder_path = os.path.dirname(file_path)
            # 使用explorer打开文件夹并选中文件
            subprocess.Popen(f'explorer /select,"{file_path}"')
        except Exception as e:
            messagebox.showerror("错误", f"无法打开文件夹:\n{str(e)}")

if __name__ == "__main__":
    root = tk.Tk()
    # 设置中文字体支持
    root.option_add("*Font", "SimHei 10")
    app = NTFSIndexSearcher(root)
    root.mainloop()