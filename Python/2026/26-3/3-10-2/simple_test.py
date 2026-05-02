#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SNMP功能测试脚本
验证设备连通性和数据采集
"""

import json
from snmp_monitor import monitor

def test_basic_functions():
    """测试基本功能"""
    print("===============================================")
    print("SNMP功能测试")
    print("===============================================")
    
    # 测试Ping功能
    print("测试1: Ping测试")
    ping_result = monitor.ping_test()
    print(f"结果: {ping_result}")
    print()
    
    # 测试设备信息
    print("测试2: 设备信息")
    device_info = monitor.get_device_info()
    print(f"设备描述: {device_info.get('sys_descr', '未知')}")
    print(f"设备名称: {device_info.get('sys_name', '未知')}")
    print()
    
    # 测试CPU使用率
    print("测试3: CPU使用率")
    cpu_usage = monitor.get_cpu_usage()
    print(f"CPU使用率: {cpu_usage}%")
    print()
    
    # 测试内存使用率
    print("测试4: 内存使用率")
    memory_usage = monitor.get_memory_usage()
    print(f"内存使用率: {memory_usage}%")
    print()
    
    # 测试运行时间
    print("测试5: 设备运行时间")
    uptime = monitor.get_uptime()
    print(f"运行时间: {uptime}")
    print()
    
    # 测试接口状态
    print("测试6: 接口状态")
    interfaces = monitor.get_interface_status()
    print(f"发现接口数量: {len(interfaces)}")
    for name, info in list(interfaces.items())[:5]:  # 只显示前5个接口
        print(f"  {name}: {info['status']} - {info['speed']}")
    print()
    
    # 测试包传输速率
    print("测试7: 包传输速率")
    packet_rates = monitor.get_packet_rates()
    print(f"接收包数: {packet_rates['in_packets']}")
    print(f"发送包数: {packet_rates['out_packets']}")
    print()
    
    # 测试完整数据采集
    print("测试8: 完整数据采集")
    all_data = monitor.collect_all_data()
    print(f"数据采集成功: {all_data['timestamp']}")
    print()
    
    # 测试历史数据
    print("测试9: 历史数据")
    history = monitor.get_history_data()
    print(f"历史数据点数量:")
    print(f"  CPU使用率: {len(history.get('cpu_usage', []))}")
    print(f"  内存使用率: {len(history.get('memory_usage', []))}")
    print(f"  包传输速率: {len(history.get('packet_rates', []))}")
    print()

def main():
    """主函数"""
    try:
        test_basic_functions()
        print("===============================================")
        print(" 所有测试完成！")
        print("===============================================")
    except Exception as e:
        print(f" 测试出错: {e}")
        print("===============================================")

if __name__ == "__main__":
    main()
