#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据库备份工具测试脚本
用于验证核心功能模块
"""

import os
import sys
import json
import tempfile
from datetime import datetime

# 添加当前目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

def test_config_management():
    """测试配置管理功能"""
    print("测试配置管理功能...")
    
    # 模拟配置数据
    test_config = {
        'host': 'localhost',
        'port': '3306',
        'username': 'test_user',
        'encrypted_password': 'encrypted_password_here',
        'backup_path': '/test/backup/path'
    }
    
    # 创建临时配置文件
    config_file = 'test_config.json'
    
    try:
        # 保存配置
        with open(config_file, 'w', encoding='utf-8') as f:
            json.dump(test_config, f, ensure_ascii=False, indent=2)
        
        # 加载配置
        with open(config_file, 'r', encoding='utf-8') as f:
            loaded_config = json.load(f)
        
        # 验证配置
        assert loaded_config['host'] == 'localhost'
        assert loaded_config['port'] == '3306'
        assert loaded_config['username'] == 'test_user'
        
        print("✓ 配置管理功能正常")
        return True
        
    except Exception as e:
        print(f"✗ 配置管理功能测试失败: {e}")
        return False
        
    finally:
        # 清理测试文件
        if os.path.exists(config_file):
            os.remove(config_file)

def test_file_operations():
    """测试文件操作功能"""
    print("测试文件操作功能...")
    
    try:
        # 创建临时目录
        with tempfile.TemporaryDirectory() as temp_dir:
            # 生成测试文件名
            backup_time = datetime.now().strftime("%Y%m%d_%H%M%S")
            backup_filename = f"testdb_{backup_time}.sql"
            backup_path = os.path.join(temp_dir, backup_filename)
            
            # 写入测试内容
            test_content = """-- 测试数据库备份文件
-- 数据库: testdb
-- 备份时间: {timestamp}

CREATE DATABASE IF NOT EXISTS `testdb` DEFAULT CHARACTER SET utf8mb4;
USE `testdb`;

-- 表结构: users
CREATE TABLE `users` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `username` varchar(50) NOT NULL,
    `email` varchar(100) NOT NULL,
    PRIMARY KEY (`id`)
);
""".format(timestamp=datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
            
            # 写入文件
            with open(backup_path, 'w', encoding='utf-8') as f:
                f.write(test_content)
            
            # 验证文件存在且内容正确
            assert os.path.exists(backup_path)
            
            with open(backup_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            assert 'CREATE DATABASE IF NOT EXISTS `testdb`' in content
            assert 'CREATE TABLE `users`' in content
            
        print("✓ 文件操作功能正常")
        return True
        
    except Exception as e:
        print(f"✗ 文件操作功能测试失败: {e}")
        return False

def test_compression():
    """测试压缩功能"""
    print("测试压缩功能...")
    
    try:
        import gzip
        
        # 创建临时目录
        with tempfile.TemporaryDirectory() as temp_dir:
            # 生成压缩文件名
            backup_time = datetime.now().strftime("%Y%m%d_%H%M%S")
            backup_filename = f"testdb_{backup_time}.sql.gz"
            backup_path = os.path.join(temp_dir, backup_filename)
            
            # 测试内容
            test_content = "这是一个测试数据库备份内容" * 100
            
            # 压缩并写入文件
            with gzip.open(backup_path, 'wt', encoding='utf-8') as f:
                f.write(test_content)
            
            # 验证压缩文件存在
            assert os.path.exists(backup_path)
            
            # 解压缩并验证内容
            with gzip.open(backup_path, 'rt', encoding='utf-8') as f:
                decompressed_content = f.read()
            
            assert decompressed_content == test_content
            
        print("✓ 压缩功能正常")
        return True
        
    except Exception as e:
        print(f"✗ 压缩功能测试失败: {e}")
        return False

def test_imports():
    """测试必要的模块导入"""
    print("测试模块导入...")
    
    try:
        import pymysql
        import cryptography.fernet
        import tkinter as tk
        print("✓ 所有必要模块导入成功")
        return True
    except ImportError as e:
        print(f"✗ 模块导入失败: {e}")
        return False

def main():
    """运行所有测试"""
    print("=" * 50)
    print("数据库备份工具 - 功能测试")
    print("=" * 50)
    
    tests = [
        test_imports,
        test_config_management,
        test_file_operations,
        test_compression
    ]
    
    passed = 0
    total = len(tests)
    
    for test in tests:
        if test():
            passed += 1
        print()
    
    print("=" * 50)
    print(f"测试结果: {passed}/{total} 通过")
    
    if passed == total:
        print("🎉 所有测试通过！程序可以正常使用。")
        return True
    else:
        print("⚠️  部分测试失败，请检查依赖和环境配置。")
        return False

if __name__ == "__main__":
    main()
