#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SNMP 常规数据采集模块（CPU / 内存 / 包发送 / 设备信息）
刷新间隔: 60秒，使用 SNMP GET 操作，轻量快速。

接口数据采集已拆分到 interface.py，两者独立运行互不阻塞。
"""

import asyncio
import os
import threading
import time
from datetime import datetime

from ping3 import ping
from pysnmp.hlapi.v1arch.asyncio import (
    get_cmd, SnmpDispatcher,
    CommunityData, UdpTransportTarget, ObjectType, ObjectIdentity
)

# ==================== OID 配置加载 ====================

def load_oid_config(config_path=None):
    """从 oids.txt 加载 OID 配置（仅 CPU/内存相关）"""
    if config_path is None:
        config_path = os.path.join(os.path.dirname(__file__), 'oids.txt')

    config = {
        'cpu': '1.3.6.1.4.1.2011.6.3.4.1.2.0.1.0',
        'memory': [
            '1.3.6.1.4.1.2011.5.25.31.1.1.1.1.7.67371017',
            '1.3.6.1.4.1.2011.5.25.31.1.1.1.1.7.67633161',
        ],
    }

    if os.path.exists(config_path):
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if 'CPU' in line and ':' in line and '1.3.6.1' in line:
                        config['cpu'] = line.split(':')[-1].strip()
                    elif '使用率' in line and ':' in line and '1.3.6.1' in line:
                        oid = line.split(':')[-1].strip()
                        if oid not in config['memory']:
                            config['memory'].append(oid)
        except Exception as e:
            print(f"[SNMPMonitor] 读取oids.txt配置出错: {e}，使用默认配置")

    return config


# ==================== 常规数据采集器 ====================

class SNMPMonitor:
    """CPU / 内存 / 包发送 / 设备信息 采集器"""

    def __init__(self, ip='172.16.100.100', community='Public123'):
        self.ip = ip
        self.community = community

        # 加载 OID 配置
        self.oid_config = load_oid_config()
        print(f"[SNMPMonitor] OID配置: CPU={self.oid_config['cpu']}")

        # 常规数据缓存（60秒刷新）
        self._cached_general_data = {}
        self._general_last_refresh = None

        # SNMP GET 超时（秒）
        self.GET_TIMEOUT = 10

        # 上次成功值缓存（SNMP 失败时保留，防止抖动）
        self._last_cpu = None
        self._last_memory = None
        self._last_in_packets = 0
        self._last_out_packets = 0

        # 历史数据（最多保存 60 条，展示 6 条）
        self.data_history = {
            'ping_status': [],
            'cpu_usage': [],
            'memory_usage': [],
            'packet_rates': []
        }
        self.max_history = 60
        self.display_history = 6

        # 独立事件循环（后台线程专用）
        self._loop = None

    # ==================== SNMP GET（带超时） ====================

    async def _async_snmp_get(self, oid):
        """执行 SNMP GET 操作（异步，带超时）"""
        try:
            task = get_cmd(
                SnmpDispatcher(),
                CommunityData(self.community),
                await UdpTransportTarget.create((self.ip, 161)),
                ObjectType(ObjectIdentity(oid))
            )
            errorIndication, errorStatus, errorIndex, varBinds = await asyncio.wait_for(
                task, timeout=self.GET_TIMEOUT
            )
            if errorIndication:
                print(f"  [SNMPMonitor] GET {oid} 错误: {errorIndication}")
                return None
            elif errorStatus:
                print(f"  [SNMPMonitor] GET {oid} 状态错误: {errorStatus.prettyPrint()}")
                return None
            else:
                for varBind in varBinds:
                    val = str(varBind[1])
                    return val
        except asyncio.TimeoutError:
            print(f"  [SNMPMonitor] GET {oid} 超时 ({self.GET_TIMEOUT}s)")
            return None
        except Exception as e:
            print(f"  [SNMPMonitor] GET {oid} 异常: {e}")
            return None
        return None

    def _snmp_get_sync(self, oid):
        """同步 SNMP GET（供监控线程调用）"""
        return asyncio.run(self._async_snmp_get(oid))

    # ==================== 设备基础信息 ====================

    def get_device_info(self):
        """获取设备基本信息"""
        info = {
            'sys_descr': self._snmp_get_sync('1.3.6.1.2.1.1.1.0') or '未知',
            'sys_name': self._snmp_get_sync('1.3.6.1.2.1.1.5.0') or '未知',
            'sys_location': self._snmp_get_sync('1.3.6.1.2.1.1.6.0') or '未知',
            'sys_contact': self._snmp_get_sync('1.3.6.1.2.1.1.4.0') or '未知',
        }
        return info

    # ==================== CPU / 内存 / 包数据 ====================

    def get_cpu_usage(self):
        """获取 CPU 使用率，失败时返回上次成功值"""
        cpu_oid = self.oid_config['cpu']
        cpu_usage = self._snmp_get_sync(cpu_oid)
        if cpu_usage:
            try:
                usage = int(cpu_usage)
                if 0 <= usage <= 100:
                    self._last_cpu = usage
                    return usage
            except (ValueError, TypeError):
                pass
        print(f"  [SNMPMonitor] CPU数据获取失败，使用缓存值: {self._last_cpu}")
        return self._last_cpu

    def get_memory_usage(self):
        """获取内存使用率，失败时返回上次成功值"""
        for oid in self.oid_config['memory']:
            mem_usage = self._snmp_get_sync(oid)
            if mem_usage:
                try:
                    usage = float(mem_usage)
                    if 0 <= usage <= 100:
                        self._last_memory = round(usage, 2)
                        return self._last_memory
                except (ValueError, TypeError):
                    continue
        print(f"  [SNMPMonitor] 内存数据获取失败，使用缓存值: {self._last_memory}")
        return self._last_memory

    def get_packet_rates(self):
        """获取 IP 层包发送/接收统计，失败时返回上次成功值"""
        in_pkts = self._snmp_get_sync('1.3.6.1.2.1.4.3.0')   # ipInReceives
        out_pkts = self._snmp_get_sync('1.3.6.1.2.1.4.10.0')  # ipOutRequests

        if in_pkts is not None:
            try:
                self._last_in_packets = int(in_pkts)
            except (ValueError, TypeError):
                pass
        if out_pkts is not None:
            try:
                self._last_out_packets = int(out_pkts)
            except (ValueError, TypeError):
                pass

        if in_pkts is None and out_pkts is None:
            print(f"  [SNMPMonitor] 包数据获取失败，使用缓存值: in={self._last_in_packets}, out={self._last_out_packets}")

        return {
            'in_packets': self._last_in_packets,
            'out_packets': self._last_out_packets
        }

    def get_uptime(self):
        """获取设备运行时间"""
        uptime = self._snmp_get_sync('1.3.6.1.2.1.1.3.0')
        if uptime:
            try:
                if '(' in uptime and ')' in uptime:
                    ticks = int(uptime.split('(')[1].split(')')[0])
                else:
                    ticks = int(uptime)
                seconds = ticks / 100
                days = seconds // 86400
                hours = (seconds % 86400) // 3600
                minutes = (seconds % 3600) // 60
                return f"{int(days)}天{int(hours)}小时{int(minutes)}分钟"
            except (ValueError, TypeError):
                return uptime
        return "未知"

    # ==================== 网络连通性 ====================

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

    # ==================== 数据采集与缓存 ====================

    def collect_general_data(self):
        """
        执行一次完整常规数据采集（监控线程调用）。
        包含: Ping / 设备信息 / CPU / 内存 / 包发送 / 运行时间
        """
        print(f"\n[{datetime.now().strftime('%H:%M:%S')}] [SNMPMonitor] 采集常规数据...")
        start = time.time()

        data = {
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'ping': self.ping_test(),
            'device_info': self.get_device_info(),
            'cpu_usage': self.get_cpu_usage(),
            'memory_usage': self.get_memory_usage(),
            'uptime': self.get_uptime(),
            'packet_rates': self.get_packet_rates(),
        }

        self._cached_general_data = data
        self._general_last_refresh = datetime.now()
        self._update_history(data)

        elapsed = round(time.time() - start, 2)
        print(f"  [SNMPMonitor] 常规数据采集完成, 耗时={elapsed}s, "
              f"CPU={data['cpu_usage']}%, MEM={data['memory_usage']}%")

        return data

    def get_cached_data(self):
        """
        获取缓存的常规数据（API 调用，不触发 SNMP）。
        若缓存为空（首次启动前），返回带默认值的结构。
        """
        if self._cached_general_data:
            return dict(self._cached_general_data)

        return {
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'ping': {'status': '初始化中', 'response_time': None},
            'device_info': {},
            'cpu_usage': self._last_cpu,
            'memory_usage': self._last_memory,
            'uptime': '未知',
            'packet_rates': {
                'in_packets': self._last_in_packets,
                'out_packets': self._last_out_packets
            },
        }

    def get_last_refresh(self):
        """返回上次常规数据刷新时间"""
        if self._general_last_refresh:
            return self._general_last_refresh.strftime('%Y-%m-%d %H:%M:%S')
        return None

    # ==================== 历史数据 ====================

    def _update_history(self, data):
        """更新历史记录"""
        ts = datetime.now()

        self.data_history['ping_status'].append({
            'time': ts, 'status': data['ping']['status'],
            'response_time': data['ping']['response_time']
        })
        self.data_history['cpu_usage'].append({
            'time': ts, 'usage': data['cpu_usage']
        })
        self.data_history['memory_usage'].append({
            'time': ts, 'usage': data['memory_usage']
        })
        self.data_history['packet_rates'].append({
            'time': ts,
            'in_packets': data['packet_rates']['in_packets'],
            'out_packets': data['packet_rates']['out_packets']
        })

        for key in self.data_history:
            if isinstance(self.data_history[key], list) and len(self.data_history[key]) > self.max_history:
                self.data_history[key] = self.data_history[key][-self.max_history:]

    def get_history_data(self, show_all=False):
        """获取历史数据"""
        if show_all:
            return self.data_history
        limited = {}
        for key, value in self.data_history.items():
            if isinstance(value, list):
                limited[key] = value[-self.display_history:]
            else:
                limited[key] = value
        return limited

    def clear_history(self):
        """清除历史数据"""
        self.data_history = {
            'ping_status': [], 'cpu_usage': [],
            'memory_usage': [], 'packet_rates': []
        }
        return True

    # ==================== 后台采集线程 ====================

    def _general_loop(self):
        """常规数据采集循环 (60秒)"""
        # 立即采集一次
        self.collect_general_data()

        while True:
            try:
                time.sleep(60)
                self.collect_general_data()
            except Exception as e:
                print(f"[SNMPMonitor] 常规采集异常: {e}")
                import traceback
                traceback.print_exc()
                time.sleep(10)  # 异常后等待 10 秒

    def start(self):
        """启动常规数据采集线程"""
        t = threading.Thread(
            target=self._general_loop,
            daemon=True,
            name="SNMPMonitor"
        )
        t.start()
        print(f"[SNMPMonitor] 采集线程已启动 (刷新间隔=60s, GET超时={self.GET_TIMEOUT}s)")
        return t


# ==================== 全局实例 ====================

monitor = SNMPMonitor()


def start_monitoring():
    """启动常规数据采集（供 web.py 调用）"""
    return monitor.start()


# ==================== 独立测试入口 ====================

if __name__ == "__main__":
    print("=" * 50)
    print("常规数据采集模块 - 独立测试")
    print("=" * 50)

    monitor.start()

    # 等待首次采集完成
    print("等待首次采集完成...")
    while monitor.get_last_refresh() is None:
        time.sleep(0.5)

    data = monitor.get_cached_data()
    print(f"\n采集完成:")
    print(f"  Ping: {data['ping']}")
    print(f"  CPU: {data['cpu_usage']}%")
    print(f"  内存: {data['memory_usage']}%")
    print(f"  运行时间: {data['uptime']}")
    print(f"  接收包: {data['packet_rates']['in_packets']}")
    print(f"  发送包: {data['packet_rates']['out_packets']}")

    print("\n测试完成！采集线程在后台继续运行...")
    print("按 Ctrl+C 退出")
