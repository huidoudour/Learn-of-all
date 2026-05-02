#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SNMP网络设备监控系统启动脚本
检查依赖并启动Web服务
"""

import subprocess
import sys
import os

def check_dependencies():
    """检查依赖包是否安装"""
    print("检查依赖包...")
    
    try:
        # 尝试导入必要的包
        import flask
        import pysnmp
        import ping3
        print("所有依赖包已安装")
        return True
    except ImportError as e:
        print(f"缺少依赖包: {e}")
        return False

def install_dependencies():
    """安装依赖包"""
    print("安装依赖包...")
    try:
        subprocess.check_call([
            sys.executable, "-m", "pip", "install", "-r", "requirements.txt"
        ])
        print("依赖包安装成功")
        return True
    except subprocess.CalledProcessError as e:
        print(f"依赖包安装失败: {e}")
        return False

def main():
    """主函数"""
    print("===============================================")
    print("SNMP网络设备监控系统")
    print("===============================================")
    
    # 检查依赖
    if not check_dependencies():
        if not install_dependencies():
            print("无法安装依赖，程序退出")
            sys.exit(1)
    
    # 显示配置信息
    print("监控配置信息:")
    print(f"   设备IP: 172.16.100.100")
    print(f"   SNMP团体字: Public123")
    print(f"   监控间隔: 10秒")
    print(f"   Web端口: 5000")
    
    # 启动Web服务
    print("启动Web服务...")
    print(f"   访问地址: http://localhost:5000")
    print(f"   按 Ctrl+C 停止服务")
    print("===============================================")
    
    try:
        # 启动Flask应用
        from app import app
        print("启动Flask应用...")
        app.run(host='0.0.0.0', port=5000, debug=True)
    except Exception as e:
        print(f"启动失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
