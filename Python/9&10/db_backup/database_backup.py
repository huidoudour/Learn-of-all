#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据库备份工具
功能：
1. 连接本地MySQL数据库（3306端口）
2. 备份数据库数据
3. 支持记忆上次备份的路径和数据库密码
"""

import os
import sys
import json
import subprocess
import tkinter as tk
from tkinter import ttk, filedialog, messagebox, simpledialog
from datetime import datetime
import pymysql
from cryptography.fernet import Fernet
import base64
import hashlib


class DatabaseBackupTool:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("数据库备份工具")
        self.root.geometry("600x500")
        
        # 配置文件路径
        self.config_file = "backup_config.json"
        self.config = self.load_config()
        
        # 创建加密密钥（用于密码加密）
        self.encryption_key = self.get_or_create_encryption_key()
        self.cipher_suite = Fernet(self.encryption_key)
        
        self.setup_ui()
        
    def get_or_create_encryption_key(self):
        """获取或创建加密密钥"""
        key_file = ".backup_key"
        if os.path.exists(key_file):
            with open(key_file, 'rb') as f:
                return f.read()
        else:
            key = Fernet.generate_key()
            with open(key_file, 'wb') as f:
                f.write(key)
            return key
    
    def load_config(self):
        """加载配置文件"""
        if os.path.exists(self.config_file):
            try:
                with open(self.config_file, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except:
                return {}
        return {}
    
    def save_config(self):
        """保存配置文件"""
        try:
            with open(self.config_file, 'w', encoding='utf-8') as f:
                json.dump(self.config, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"保存配置失败: {e}")
    
    def encrypt_password(self, password):
        """加密密码"""
        try:
            encrypted = self.cipher_suite.encrypt(password.encode())
            return base64.b64encode(encrypted).decode()
        except:
            return password
    
    def decrypt_password(self, encrypted_password):
        """解密密码"""
        try:
            encrypted_bytes = base64.b64decode(encrypted_password.encode())
            decrypted = self.cipher_suite.decrypt(encrypted_bytes)
            return decrypted.decode()
        except:
            return encrypted_password
    
    def setup_ui(self):
        """设置用户界面"""
        # 主框架
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # 数据库连接配置
        conn_frame = ttk.LabelFrame(main_frame, text="数据库连接配置", padding="10")
        conn_frame.grid(row=0, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        # 数据库主机
        ttk.Label(conn_frame, text="主机地址:").grid(row=0, column=0, sticky=tk.W, pady=2)
        self.host_var = tk.StringVar(value=self.config.get('host', 'localhost'))
        ttk.Entry(conn_frame, textvariable=self.host_var, width=20).grid(row=0, column=1, padx=(10, 0), pady=2)
        
        # 数据库端口
        ttk.Label(conn_frame, text="端口:").grid(row=0, column=2, sticky=tk.W, padx=(20, 0), pady=2)
        self.port_var = tk.StringVar(value=self.config.get('port', '3306'))
        ttk.Entry(conn_frame, textvariable=self.port_var, width=10).grid(row=0, column=3, padx=(10, 0), pady=2)
        
        # 数据库用户名
        ttk.Label(conn_frame, text="用户名:").grid(row=1, column=0, sticky=tk.W, pady=2)
        self.username_var = tk.StringVar(value=self.config.get('username', ''))
        ttk.Entry(conn_frame, textvariable=self.username_var, width=20).grid(row=1, column=1, padx=(10, 0), pady=2)
        
        # 数据库密码
        ttk.Label(conn_frame, text="密码:").grid(row=1, column=2, sticky=tk.W, padx=(20, 0), pady=2)
        self.password_var = tk.StringVar(value=self.config.get('encrypted_password', ''))
        if self.password_var.get():
            self.password_var.set(self.decrypt_password(self.password_var.get()))
        password_entry = ttk.Entry(conn_frame, textvariable=self.password_var, show="*", width=20)
        password_entry.grid(row=1, column=3, padx=(10, 0), pady=2)
        
        # 测试连接按钮
        ttk.Button(conn_frame, text="测试连接", command=self.test_connection).grid(row=2, column=0, columnspan=4, pady=10)
        
        # 数据库选择
        db_frame = ttk.LabelFrame(main_frame, text="数据库选择", padding="10")
        db_frame.grid(row=1, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        ttk.Label(db_frame, text="选择要备份的数据库:").grid(row=0, column=0, sticky=tk.W, pady=2)
        self.db_var = tk.StringVar()
        self.db_combo = ttk.Combobox(db_frame, textvariable=self.db_var, width=30)
        self.db_combo.grid(row=0, column=1, padx=(10, 0), pady=2)
        ttk.Button(db_frame, text="刷新", command=self.refresh_databases).grid(row=0, column=2, padx=(10, 0), pady=2)
        
        # 备份路径配置
        path_frame = ttk.LabelFrame(main_frame, text="备份路径配置", padding="10")
        path_frame.grid(row=2, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        ttk.Label(path_frame, text="备份路径:").grid(row=0, column=0, sticky=tk.W, pady=2)
        self.backup_path_var = tk.StringVar(value=self.config.get('backup_path', ''))
        ttk.Entry(path_frame, textvariable=self.backup_path_var, width=50).grid(row=0, column=1, padx=(10, 0), pady=2)
        ttk.Button(path_frame, text="浏览", command=self.select_backup_path).grid(row=0, column=2, padx=(10, 0), pady=2)
        
        # 备份选项
        options_frame = ttk.LabelFrame(main_frame, text="备份选项", padding="10")
        options_frame.grid(row=3, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))
        
        self.structure_var = tk.BooleanVar(value=True)
        ttk.Checkbutton(options_frame, text="包含表结构", variable=self.structure_var).grid(row=0, column=0, sticky=tk.W)
        
        self.data_var = tk.BooleanVar(value=True)
        ttk.Checkbutton(options_frame, text="包含数据", variable=self.data_var).grid(row=0, column=1, sticky=tk.W)
        
        self.compress_var = tk.BooleanVar(value=True)
        ttk.Checkbutton(options_frame, text="压缩备份文件", variable=self.compress_var).grid(row=0, column=2, sticky=tk.W)
        
        # 操作按钮
        button_frame = ttk.Frame(main_frame)
        button_frame.grid(row=4, column=0, columnspan=2, pady=20)
        
        ttk.Button(button_frame, text="开始备份", command=self.start_backup).grid(row=0, column=0, padx=5)
        ttk.Button(button_frame, text="保存配置", command=self.save_current_config).grid(row=0, column=1, padx=5)
        ttk.Button(button_frame, text="重置配置", command=self.reset_config).grid(row=0, column=2, padx=5)
        
        # 状态显示
        self.status_var = tk.StringVar(value="就绪")
        status_label = ttk.Label(main_frame, textvariable=self.status_var)
        status_label.grid(row=5, column=0, columnspan=2, pady=10)
        
        # 进度条
        self.progress = ttk.Progressbar(main_frame, mode='indeterminate')
        self.progress.grid(row=6, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=5)
        
        # 日志显示
        log_frame = ttk.LabelFrame(main_frame, text="操作日志", padding="10")
        log_frame.grid(row=7, column=0, columnspan=2, sticky=(tk.W, tk.E, tk.N, tk.S), pady=(0, 10))
        
        self.log_text = tk.Text(log_frame, height=8, width=70)
        scrollbar = ttk.Scrollbar(log_frame, orient=tk.VERTICAL, command=self.log_text.yview)
        self.log_text.configure(yscrollcommand=scrollbar.set)
        
        self.log_text.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        scrollbar.grid(row=0, column=1, sticky=(tk.N, tk.S))
        
        # 配置网格权重
        main_frame.columnconfigure(0, weight=1)
        main_frame.rowconfigure(7, weight=1)
        conn_frame.columnconfigure(1, weight=1)
        conn_frame.columnconfigure(3, weight=1)
        path_frame.columnconfigure(1, weight=1)
        log_frame.columnconfigure(0, weight=1)
        log_frame.rowconfigure(0, weight=1)
    
    def log_message(self, message):
        """添加日志消息"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log_entry = f"[{timestamp}] {message}\n"
        self.log_text.insert(tk.END, log_entry)
        self.log_text.see(tk.END)
        self.root.update()
    
    def test_connection(self):
        """测试数据库连接"""
        try:
            self.log_message("正在测试数据库连接...")
            connection = pymysql.connect(
                host=self.host_var.get(),
                port=int(self.port_var.get()),
                user=self.username_var.get(),
                password=self.password_var.get(),
                charset='utf8mb4'
            )
            
            with connection.cursor() as cursor:
                cursor.execute("SHOW DATABASES")
                databases = [db[0] for db in cursor.fetchall() if db[0] not in ['information_schema', 'performance_schema', 'mysql', 'sys']]
                
                # 更新数据库列表
                self.db_combo['values'] = databases
                if databases:
                    self.db_combo.set(databases[0])
            
            connection.close()
            self.status_var.set("连接成功")
            self.log_message("数据库连接成功")
            messagebox.showinfo("成功", "数据库连接成功！")
            
        except Exception as e:
            error_msg = f"连接失败: {str(e)}"
            self.status_var.set("连接失败")
            self.log_message(error_msg)
            messagebox.showerror("错误", error_msg)
    
    def refresh_databases(self):
        """刷新数据库列表"""
        self.test_connection()
    
    def select_backup_path(self):
        """选择备份路径"""
        directory = filedialog.askdirectory(initialdir=self.backup_path_var.get())
        if directory:
            self.backup_path_var.set(directory)
    
    def save_current_config(self):
        """保存当前配置"""
        try:
            self.config = {
                'host': self.host_var.get(),
                'port': self.port_var.get(),
                'username': self.username_var.get(),
                'encrypted_password': self.encrypt_password(self.password_var.get()),
                'backup_path': self.backup_path_var.get()
            }
            self.save_config()
            self.log_message("配置已保存")
            messagebox.showinfo("成功", "配置已保存！")
        except Exception as e:
            error_msg = f"保存配置失败: {str(e)}"
            self.log_message(error_msg)
            messagebox.showerror("错误", error_msg)
    
    def reset_config(self):
        """重置配置"""
        if messagebox.askyesno("确认", "确定要重置所有配置吗？\n这将清除保存的用户名、密码等信息。"):
            if os.path.exists(self.config_file):
                os.remove(self.config_file)
            self.config = {}
            
            # 重置界面值
            self.host_var.set('localhost')
            self.port_var.set('3306')
            self.username_var.set('')
            self.password_var.set('')
            self.backup_path_var.set('')
            self.db_combo.set('')
            
            self.log_message("配置已重置")
            messagebox.showinfo("成功", "配置已重置！")
    
    def get_table_structure(self, cursor, database, table):
        """获取表结构"""
        try:
            cursor.execute(f"USE `{database}`")
            cursor.execute(f"SHOW CREATE TABLE `{table}`")
            result = cursor.fetchone()
            return result[1] if result else ""
        except Exception as e:
            self.log_message(f"获取表 {table} 结构失败: {str(e)}")
            return ""
    
    def get_table_data(self, cursor, database, table):
        """获取表数据"""
        try:
            cursor.execute(f"USE `{database}`")
            cursor.execute(f"SELECT * FROM `{table}`")
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            return columns, rows
        except Exception as e:
            self.log_message(f"获取表 {table} 数据失败: {str(e)}")
            return [], []
    
    def start_backup(self):
        """开始备份"""
        if not self.db_var.get():
            messagebox.showerror("错误", "请选择要备份的数据库！")
            return
        
        if not self.backup_path_var.get():
            messagebox.showerror("错误", "请选择备份路径！")
            return
        
        # 保存配置
        self.save_current_config()
        
        # 开始备份
        self.progress.start()
        self.status_var.set("正在备份...")
        
        try:
            # 创建连接
            connection = pymysql.connect(
                host=self.host_var.get(),
                port=int(self.port_var.get()),
                user=self.username_var.get(),
                password=self.password_var.get(),
                charset='utf8mb4'
            )
            
            with connection.cursor() as cursor:
                database = self.db_var.get()
                backup_time = datetime.now().strftime("%Y%m%d_%H%M%S")
                backup_filename = f"{database}_{backup_time}.sql"
                
                if self.compress_var.get():
                    backup_filename += ".gz"
                
                backup_path = os.path.join(self.backup_path_var.get(), backup_filename)
                
                self.log_message(f"开始备份数据库: {database}")
                self.log_message(f"备份文件: {backup_path}")
                
                # 使用mysqldump进行备份（推荐方式）
                if self.use_mysqldump_backup(cursor, database, backup_path):
                    self.log_message("备份成功完成！")
                    self.status_var.set("备份完成")
                    messagebox.showinfo("成功", f"数据库备份成功！\n备份文件: {backup_path}")
                else:
                    # 如果mysqldump失败，使用Python方式备份
                    self.log_message("mysqldump方式备份失败，使用Python方式...")
                    if self.use_python_backup(cursor, database, backup_path):
                        self.log_message("备份成功完成！")
                        self.status_var.set("备份完成")
                        messagebox.showinfo("成功", f"数据库备份成功！\n备份文件: {backup_path}")
                    else:
                        raise Exception("所有备份方式都失败了")
        
        except Exception as e:
            error_msg = f"备份失败: {str(e)}"
            self.status_var.set("备份失败")
            self.log_message(error_msg)
            messagebox.showerror("错误", error_msg)
        
        finally:
            self.progress.stop()
            try:
                connection.close()
            except:
                pass
    
    def use_mysqldump_backup(self, cursor, database, backup_path):
        """使用mysqldump进行备份"""
        try:
            # 构建mysqldump命令
            cmd = [
                'mysqldump',
                '-h', self.host_var.get(),
                '-P', self.port_var.get(),
                '-u', self.username_var.get(),
                f'-p{self.password_var.get()}',
                '--single-transaction',
                '--routines',
                '--triggers',
                database
            ]
            
            if self.compress_var.get():
                # 使用gzip压缩
                import gzip
                with open(backup_path, 'wb') as f:
                    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                    compressed = gzip.GzipFile(fileobj=f, mode='wb')
                    
                    while True:
                        chunk = process.stdout.read(8192)
                        if not chunk:
                            break
                        compressed.write(chunk)
                    
                    compressed.close()
                    process.wait()
                    
                    if process.returncode != 0:
                        stderr = process.stderr.read().decode()
                        self.log_message(f"mysqldump错误: {stderr}")
                        return False
            else:
                # 直接写入文件
                with open(backup_path, 'w', encoding='utf-8') as f:
                    process = subprocess.Popen(cmd, stdout=f, stderr=subprocess.PIPE)
                    process.wait()
                    
                    if process.returncode != 0:
                        stderr = process.stderr.read().decode()
                        self.log_message(f"mysqldump错误: {stderr}")
                        return False
            
            return True
            
        except FileNotFoundError:
            self.log_message("mysqldump命令未找到，将使用Python方式备份")
            return False
        except Exception as e:
            self.log_message(f"mysqldump备份失败: {str(e)}")
            return False
    
    def use_python_backup(self, cursor, database, backup_path):
        """使用Python方式备份"""
        try:
            # 获取所有表
            cursor.execute(f"USE `{database}`")
            cursor.execute("SHOW TABLES")
            tables = [table[0] for table in cursor.fetchall()]
            
            if not tables:
                raise Exception(f"数据库 {database} 中没有找到表")
            
            backup_content = []
            
            # 添加数据库创建语句
            backup_content.append(f"-- 数据库备份文件")
            backup_content.append(f"-- 数据库: {database}")
            backup_content.append(f"-- 备份时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
            backup_content.append("")
            backup_content.append(f"CREATE DATABASE IF NOT EXISTS `{database}` DEFAULT CHARACTER SET utf8mb4;")
            backup_content.append(f"USE `{database}`;")
            backup_content.append("")
            
            # 备份每个表
            for i, table in enumerate(tables):
                self.log_message(f"正在备份表 {i+1}/{len(tables)}: {table}")
                
                # 获取表结构
                if self.structure_var.get():
                    structure = self.get_table_structure(cursor, database, table)
                    if structure:
                        backup_content.append(f"-- 表结构: {table}")
                        backup_content.append(f"DROP TABLE IF EXISTS `{table}`;")
                        backup_content.append(structure + ";")
                        backup_content.append("")
                
                # 获取表数据
                if self.data_var.get():
                    columns, rows = self.get_table_data(cursor, database, table)
                    if rows:
                        backup_content.append(f"-- 表数据: {table}")
                        backup_content.append(f"INSERT INTO `{table}` ({', '.join([f'`{col}`' for col in columns])}) VALUES")
                        
                        for row_idx, row in enumerate(rows):
                            values = []
                            for value in row:
                                if value is None:
                                    values.append('NULL')
                                elif isinstance(value, str):
                                    # 转义单引号
                                    escaped_value = value.replace("'", "''")
                                    values.append(f"'{escaped_value}'")
                                else:
                                    values.append(str(value))
                            
                            if row_idx == len(rows) - 1:
                                backup_content.append(f"({', '.join(values)});")
                            else:
                                backup_content.append(f"({', '.join(values)}),")
                        backup_content.append("")
            
            # 写入文件
            if self.compress_var.get():
                import gzip
                with gzip.open(backup_path, 'wt', encoding='utf-8') as f:
                    f.write('\n'.join(backup_content))
            else:
                with open(backup_path, 'w', encoding='utf-8') as f:
                    f.write('\n'.join(backup_content))
            
            return True
            
        except Exception as e:
            self.log_message(f"Python备份失败: {str(e)}")
            return False
    
    def run(self):
        """运行应用程序"""
        self.root.mainloop()


def main():
    """主函数"""
    try:
        app = DatabaseBackupTool()
        app.run()
    except Exception as e:
        print(f"启动失败: {e}")
        input("按Enter键退出...")


if __name__ == "__main__":
    main()
