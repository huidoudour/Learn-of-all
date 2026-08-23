#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
简单测试脚本 - 验证基本功能
"""

import os
import sys

def check_python_version():
    """检查Python版本"""
    print(" Python版本检查:")
    print(f"   版本: {sys.version}")
    print(f"   路径: {sys.executable}")
    
    if sys.version_info >= (3, 7):
        print("    Python版本符合要求")
        return True
    else:
        print("    Python版本过低，需要3.7+")
        return False

def check_files():
    """检查必要文件是否存在"""
    print("\n 文件检查:")
    
    required_files = [
        'app.py',
        'snmp_monitor.py', 
        'run.py',
        'requirements.txt',
        'templates/index.html'
    ]
    
    all_exists = True
    for file in required_files:
        if os.path.exists(file):
            print(f"    {file}")
        else:
            print(f"    {file} - 文件缺失")
            all_exists = False
    
    return all_exists

def check_imports():
    """检查基本导入"""
    print("\n 导入检查:")
    
    # 基本Python模块
    basic_modules = ['json', 'time', 'threading', 'datetime']
    
    for module in basic_modules:
        try:
            __import__(module)
            print(f"    {module}")
        except ImportError:
            print(f"    {module}")
            return False
    
    # 可选模块（需要安装）
    optional_modules = ['flask', 'pysnmp', 'ping3']
    
    for module in optional_modules:
        try:
            __import__(module)
            print(f"    {module} (已安装)")
        except ImportError:
            print(f"   ️  {module} (需要安装)")
    
    return True

def main():
    """主测试函数"""
    print(" SNMP监控系统 - 环境检查")
    print("=" * 50)
    
    # 检查当前目录
    current_dir = os.getcwd()
    print(f"当前目录: {current_dir}")
    
    # 执行检查
    version_ok = check_python_version()
    files_ok = check_files()
    imports_ok = check_imports()
    
    print("\n" + "=" * 50)
    print(" 检查结果汇总:")
    
    if version_ok and files_ok and imports_ok:
        print(" 基本环境检查通过")
        print("\n下一步操作:")
        print("1. 安装依赖: pip install -r requirements.txt")
        print("2. 启动服务: python run.py")
        print("3. 访问: http://localhost:5000")
    else:
        print(" 环境检查未通过")
        
        if not version_ok:
            print("   - 请升级Python到3.7+")
        if not files_ok:
            print("   - 请检查项目文件完整性")
    
    print("\n 提示:")
    print("   确保设备 192.168.8.1 可访问")
    print("   确保SNMP服务已启用，团体字为 Public123")

if __name__ == "__main__":
    main()
