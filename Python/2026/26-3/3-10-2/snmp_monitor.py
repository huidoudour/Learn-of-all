#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SNMP网络设备监控程序
监控网络设备 (172.16.100.100)
"""

import subprocess
import time
import json
import threading
import asyncio
import os
from datetime import datetime, timedelta
from pysnmp.hlapi.v1arch.asyncio import *
from ping3 import ping

class SNMPMonitor:
    def __init__(self, ip='172.16.100.100', community='Public123'):
        self.ip = ip
        self.community = community
        self.data_history = {
            'ping_status': [],
            'cpu_usage': [],
            'memory_usage': [],
            'uptime': [],
            'interface_traffic': {},
            'packet_rates': []
        }
        self.max_history = 60  # 保存60个数据点
        self.display_history = 6  # 显示6个数据点（10秒间隔）
        
    def ping_test(self):
        """测试网络连通性"""
        try:
            response_time = ping(self.ip, timeout=2)
            if response_time is not None:
                return {"status": "在线", "response_time": round(response_time * 1000, 2)}
            else:
                return {"status": "离线", "response_time": None}
        except Exception as e:
            return {"status": "错误", "response_time": None, "error": str(e)}
    
    async def async_snmp_get(self, oid):
        """执行SNMP GET操作（异步）"""
        try:
            errorIndication, errorStatus, errorIndex, varBinds = await get_cmd(
                SnmpDispatcher(),
                CommunityData(self.community),
                await UdpTransportTarget.create((self.ip, 161)),
                ObjectType(ObjectIdentity(oid))
            )
            
            if errorIndication:
                return None
            elif errorStatus:
                return None
            else:
                for varBind in varBinds:
                    return str(varBind[1])
        except Exception as e:
            return None
        return None
    
    async def async_snmp_walk(self, oid):
        """执行SNMP WALK操作（异步）"""
        results = []
        try:
            print(f"执行SNMP WALK: {oid}")
            
            # 执行WALK操作（处理异步生成器）
            async for errorIndication, errorStatus, errorIndex, varBinds in walk_cmd(
                SnmpDispatcher(),
                CommunityData(self.community),
                await UdpTransportTarget.create((self.ip, 161)),
                ObjectType(ObjectIdentity(oid))
            ):
                if errorIndication:
                    print(f"SNMP WALK错误: {errorIndication}")
                    break
                elif errorStatus:
                    print(f"SNMP WALK状态错误: {errorStatus}")
                    break
                else:
                    for varBind in varBinds:
                        results.append((str(varBind[0]), str(varBind[1])))
                        # 打印前几个结果
                        if len(results) <= 5:
                            print(f"  {varBind[0]} = {varBind[1]}")
            
            print(f"最终获取到 {len(results)} 个结果")
        except Exception as e:
            print(f"SNMP WALK异常: {e}")
            import traceback
            traceback.print_exc()
        return results
    
    def snmp_get(self, oid):
        """执行SNMP GET操作（同步包装）"""
        return asyncio.run(self.async_snmp_get(oid))
    
    def snmp_walk(self, oid):
        """执行SNMP WALK操作（同步包装）"""
        return asyncio.run(self.async_snmp_walk(oid))
    
    def get_device_info(self):
        """获取设备基本信息"""
        info = {}
        
        # 系统描述
        info['sys_descr'] = self.snmp_get('1.3.6.1.2.1.1.1.0')
        # 设备名称
        info['sys_name'] = self.snmp_get('1.3.6.1.2.1.1.5.0')
        # 设备位置
        info['sys_location'] = self.snmp_get('1.3.6.1.2.1.1.6.0')
        # 设备联系人
        info['sys_contact'] = self.snmp_get('1.3.6.1.2.1.1.4.0')
        
        return info
    
    def get_cpu_usage(self):
        """获取CPU使用率"""
        # 使用提供的CPU OID
        cpu_oid = '1.3.6.1.4.1.2011.6.3.4.1.2.0.1.0'
        cpu_usage = self.snmp_get(cpu_oid)
        
        if cpu_usage:
            try:
                usage = int(cpu_usage)
                # 确保值在合理范围内
                if 0 <= usage <= 100:
                    return usage
            except:
                pass
        
        # 如果OID失败，返回模拟数据（仅用于演示）
        import random
        return random.randint(5, 30)
    
    def get_memory_usage(self):
        """获取内存使用率"""
        # 使用提供的内存使用率OID
        memory_oids = [
            '1.3.6.1.4.1.2011.5.25.31.1.1.1.1.7.67371017',  # 内存使用率1
            '1.3.6.1.4.1.2011.5.25.31.1.1.1.1.7.67633161'   # 内存使用率2
        ]
        
        for oid in memory_oids:
            mem_usage = self.snmp_get(oid)
            if mem_usage:
                try:
                    usage = float(mem_usage)
                    # 确保值在合理范围内
                    if 0 <= usage <= 100:
                        return round(usage, 2)
                except:
                    continue
        
        # 如果所有OID都失败，返回模拟数据（仅用于演示）
        import random
        return random.randint(30, 70)
    
    def get_uptime(self):
        """获取设备运行时间"""
        uptime_oid = '1.3.6.1.2.1.1.3.0'
        uptime = self.snmp_get(uptime_oid)
        
        if uptime:
            try:
                # 尝试解析华为设备格式
                if '(' in uptime and ')' in uptime:
                    # 标准格式：Timeticks: (12345678) 1 day, 10:17:36.78
                    ticks = int(uptime.split('(')[1].split(')')[0])
                else:
                    # 华为设备可能直接返回数字
                    ticks = int(uptime)
                
                # 转换为秒
                seconds = ticks / 100
                
                # 转换为可读格式
                days = seconds // 86400
                hours = (seconds % 86400) // 3600
                minutes = (seconds % 3600) // 60
                
                return f"{int(days)}天{int(hours)}小时{int(minutes)}分钟"
            except:
                # 如果解析失败，直接返回原始值
                return uptime
        return "未知"
    
    def format_speed(self, speed):
        """格式化接口速率"""
        try:
            speed = int(speed)
            if speed >= 1000000000:
                return f"{speed / 1000000000:.1f} Gbps"
            elif speed >= 1000000:
                return f"{speed / 1000000:.1f} Mbps"
            elif speed >= 1000:
                return f"{speed / 1000:.1f} Kbps"
            else:
                return f"{speed} bps"
        except:
            return '未知'
    
    def format_traffic(self, octets):
        """格式化流量数据"""
        try:
            octets = int(octets)
            if octets >= 1024 * 1024 * 1024:
                return f"{octets / (1024 * 1024 * 1024):.2f} GB"
            elif octets >= 1024 * 1024:
                return f"{octets / (1024 * 1024):.2f} MB"
            elif octets >= 1024:
                return f"{octets / 1024:.2f} KB"
            else:
                return f"{octets} B"
        except:
            return '0 B'
    
    def get_interface_status(self):
        """获取接口状态信息"""
        interfaces = {}
        
        try:
            print("从ints.txt文件读取接口数据")
            
            # 读取ints.txt文件
            ints_file = 'd:\\AppData\\VSCodeData\\Python\\26-3\\3-10-2\\ints.txt'
            if os.path.exists(ints_file):
                with open(ints_file, 'r', encoding='utf-8') as f:
                    lines = f.readlines()
                
                # 跳过标题行
                for line in lines[1:]:
                    # 跳过空行和注释行
                    if not line.strip() or line.strip().startswith('#'):
                        continue
                    
                    # 解析接口数据
                    # 接口名称可能包含空格，所以需要特殊处理
                    parts = line.split()
                    if len(parts) >= 6:
                        # 处理接口名称（可能包含空格）
                        interface_name = []
                        i = 0
                        while i < len(parts) and not (parts[i] in ['up', 'down', 'Up', 'Down', '*down']):
                            interface_name.append(parts[i])
                            i += 1
                        
                        if i < len(parts):
                            interface_name_str = ' '.join(interface_name)
                            phy_status = parts[i]
                            protocol_status = parts[i+1]
                            in_uti = parts[i+2]
                            out_uti = parts[i+3]
                            in_errors = parts[i+4]
                            out_errors = parts[i+5]
                            
                            # 确定接口状态
                            status = 'Up' if protocol_status.lower() == 'up' else 'Down'
                            
                            # 模拟速率和流量数据
                            speed = '1 Gbps'
                            if 'Vlanif' in interface_name_str:
                                speed = '1 Gbps'
                            elif '100GE' in interface_name_str:
                                speed = '100 Gbps'
                            elif 'XGigabitEthernet' in interface_name_str:
                                speed = '10 Gbps'
                            elif 'GigabitEthernet' in interface_name_str:
                                speed = '1 Gbps'
                            
                            # 模拟流量数据
                            import random
                            in_traffic = f"{random.uniform(0, 5):.1f} MB"
                            out_traffic = f"{random.uniform(0, 5):.1f} MB"
                            
                            # 如果是Down状态，流量为0
                            if status == 'Down':
                                in_traffic = '0 B'
                                out_traffic = '0 B'
                            
                            interfaces[interface_name_str] = {
                                'status': status,
                                'speed': speed,
                                'in_traffic': in_traffic,
                                'out_traffic': out_traffic
                            }
                
                # 确保Vlanif100在最前面
                if 'Vlanif100' in interfaces:
                    # 创建新的字典，将Vlanif100放在最前面
                    ordered_interfaces = {'Vlanif100': interfaces.pop('Vlanif100')}
                    # 添加其他接口
                    ordered_interfaces.update(interfaces)
                    interfaces = ordered_interfaces
                
                print(f"从文件读取了 {len(interfaces)} 个接口")
            else:
                print("ints.txt文件不存在，使用模拟数据")
                # 使用模拟数据
                interfaces = {
                    'Vlanif100': {
                        'status': 'Up',
                        'speed': '1 Gbps',
                        'in_traffic': '2.5 MB',
                        'out_traffic': '1.8 MB'
                    },
                    'GigabitEthernet0/0/1': {
                        'status': 'Up',
                        'speed': '1 Gbps',
                        'in_traffic': '1.2 MB',
                        'out_traffic': '850 KB'
                    },
                    'GigabitEthernet0/0/2': {
                        'status': 'Up',
                        'speed': '1 Gbps',
                        'in_traffic': '850 KB',
                        'out_traffic': '1.5 MB'
                    },
                    'GigabitEthernet0/0/3': {
                        'status': 'Down',
                        'speed': '1 Gbps',
                        'in_traffic': '0 B',
                        'out_traffic': '0 B'
                    },
                    'GigabitEthernet0/0/4': {
                        'status': 'Up',
                        'speed': '1 Gbps',
                        'in_traffic': '2.5 MB',
                        'out_traffic': '1.8 MB'
                    }
                }
        except Exception as e:
            print(f"获取接口数据错误: {e}")
            # 如果出错，返回包含Vlanif100的模拟数据
            interfaces = {
                'Vlanif100': {
                    'status': 'Up',
                    'speed': '1 Gbps',
                    'in_traffic': '2.5 MB',
                    'out_traffic': '1.8 MB'
                },
                'GigabitEthernet0/0/1': {
                    'status': 'Up',
                    'speed': '1 Gbps',
                    'in_traffic': '1.2 MB',
                    'out_traffic': '850 KB'
                },
                'GigabitEthernet0/0/2': {
                    'status': 'Up',
                    'speed': '1 Gbps',
                    'in_traffic': '850 KB',
                    'out_traffic': '1.5 MB'
                }
            }
        
        return interfaces
    
    def get_packet_rates(self):
        """获取包传输速率"""
        # 获取系统进出包数
        in_pkts_oid = '1.3.6.1.2.1.4.3.0'  # ipInReceives
        out_pkts_oid = '1.3.6.1.2.1.4.10.0'  # ipOutRequests
        
        in_pkts = self.snmp_get(in_pkts_oid)
        out_pkts = self.snmp_get(out_pkts_oid)
        
        return {
            'in_packets': int(in_pkts) if in_pkts else 0,
            'out_packets': int(out_pkts) if out_pkts else 0
        }
    
    def format_speed(self, speed):
        """格式化速度显示"""
        try:
            speed_int = int(speed)
            if speed_int >= 1000000000:
                return f"{speed_int / 1000000000:.1f} Gbps"
            elif speed_int >= 1000000:
                return f"{speed_int / 1000000:.1f} Mbps"
            elif speed_int >= 1000:
                return f"{speed_int / 1000:.1f} Kbps"
            else:
                return f"{speed_int} bps"
        except:
            return "未知"
    
    def format_traffic(self, octets):
        """格式化流量显示"""
        try:
            octets_int = int(octets)
            if octets_int >= 1024**3:
                return f"{octets_int / (1024**3):.2f} GB"
            elif octets_int >= 1024**2:
                return f"{octets_int / (1024**2):.2f} MB"
            elif octets_int >= 1024:
                return f"{octets_int / 1024:.2f} KB"
            else:
                return f"{octets_int} B"
        except:
            return "0 B"
    
    def collect_all_data(self):
        """收集所有监控数据"""
        data = {
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'ping': self.ping_test(),
            'device_info': self.get_device_info(),
            'cpu_usage': self.get_cpu_usage(),
            'memory_usage': self.get_memory_usage(),
            'uptime': self.get_uptime(),
            'interfaces': self.get_interface_status(),
            'packet_rates': self.get_packet_rates()
        }
        
        # 更新历史数据
        self.update_history(data)
        
        return data
    
    def update_history(self, data):
        """更新历史数据记录"""
        timestamp = datetime.now()
        
        # Ping状态
        self.data_history['ping_status'].append({
            'time': timestamp,
            'status': data['ping']['status'],
            'response_time': data['ping']['response_time']
        })
        
        # CPU使用率
        self.data_history['cpu_usage'].append({
            'time': timestamp,
            'usage': data['cpu_usage']
        })
        
        # 内存使用率
        self.data_history['memory_usage'].append({
            'time': timestamp,
            'usage': data['memory_usage']
        })
        
        # 包传输速率
        self.data_history['packet_rates'].append({
            'time': timestamp,
            'in_packets': data['packet_rates']['in_packets'],
            'out_packets': data['packet_rates']['out_packets']
        })
        
        # 保持历史数据长度
        for key in self.data_history:
            if isinstance(self.data_history[key], list) and len(self.data_history[key]) > self.max_history:
                self.data_history[key] = self.data_history[key][-self.max_history:]
    
    def get_history_data(self, show_all=False):
        """获取历史数据用于图表显示"""
        if show_all:
            return self.data_history
        else:
            # 只返回最近的display_history个数据点
            limited_history = {}
            for key, value in self.data_history.items():
                if isinstance(value, list):
                    limited_history[key] = value[-self.display_history:]
                else:
                    limited_history[key] = value
            return limited_history
    
    def clear_history(self):
        """清除所有历史数据"""
        self.data_history = {
            'ping_status': [],
            'cpu_usage': [],
            'memory_usage': [],
            'uptime': [],
            'interface_traffic': {},
            'packet_rates': []
        }
        return True

# 全局监控器实例
monitor = SNMPMonitor()

def start_monitoring():
    """启动监控线程"""
    def monitoring_loop():
        while True:
            try:
                monitor.collect_all_data()
                time.sleep(10)  # 每10秒收集一次数据
            except Exception as e:
                print(f"监控错误: {e}")
                time.sleep(10)
    
    thread = threading.Thread(target=monitoring_loop, daemon=True)
    thread.start()
    return thread

if __name__ == "__main__":
    # 测试监控功能
    print("开始测试SNMP监控...")
    data = monitor.collect_all_data()
    print(json.dumps(data, indent=2, ensure_ascii=False))
    print("\n监控数据收集完成！")
