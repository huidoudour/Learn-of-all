#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SNMP网络设备监控系统 - 启动脚本
"""

import os
import sys
import subprocess
import time

def check_dependencies():
    """检查依赖包是否已安装"""
    required_packages = [
        'flask',
        'pysnmp', 
        'ping3'
    ]
    
    missing_packages = []
    
    for package in required_packages:
        try:
            __import__(package)
        except ImportError:
            missing_packages.append(package)
    
    return missing_packages

def install_dependencies():
    """安装依赖包"""
    print("正在安装依赖包...")
    
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", "-r", "requirements.txt"])
        print(" 依赖包安装成功！")
        return True
    except subprocess.CalledProcessError as e:
        print(f" 依赖包安装失败: {e}")
        return False

def main():
    """主函数"""
    print(" SNMP网络设备监控系统")
    print("=" * 50)
    
    # 检查依赖
    missing = check_dependencies()
    if missing:
        print(f"检测到缺少的依赖包: {', '.join(missing)}")
        if install_dependencies():
            print(" 所有依赖包已安装完成！")
        else:
            print(" 依赖包安装失败，请手动安装:")
            print("pip install flask pysnmp ping3")
            return
    else:
        print(" 所有依赖包已安装")
    
    print("\n 监控配置信息:")
    print("   设备IP: 192.168.8.1")
    print("   SNMP团体字: Public123")
    print("   监控间隔: 30秒")
    print("   Web端口: 5000")
    
    print("\n 启动Web服务...")
    print("   访问地址: http://localhost:5000")
    print("   按 Ctrl+C 停止服务")
    print("=" * 50)
    
    # 启动Flask应用
    try:
        from app import app
        app.run(host='0.0.0.0', port=5000, debug=False)
    except KeyboardInterrupt:
        print("\n\n 监控服务已停止")
    except Exception as e:
        print(f"\n 启动失败: {e}")

if __name__ == "__main__":
    main()
