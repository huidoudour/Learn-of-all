#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
接口数据采集模块 - 硬采集版本
通过解析 int_*.log 获取已知的 ifIndex 列表，然后对每个 ifIndex 逐条 SNMP GET
（名称/速率/状态），彻底绕开 WALK/GetBulk 兼容性问题。

数据流：
1. 解析 int_*.log → 提取所有已知 ifIndex
2. 对每个 ifIndex 并发 SNMP GET: ifDescr.{i}, ifSpeed.{i}, ifOperStatus.{i}
3. 按 ifIndex 合并 → 结构化接口信息 → 缓存
"""

import asyncio
import glob
import os
import re
import time
import threading
from datetime import datetime

from pysnmp.hlapi.v1arch.asyncio import (
    get_cmd, next_cmd, SnmpDispatcher,
    CommunityData, UdpTransportTarget, ObjectType, ObjectIdentity
)


# ==================== 工具函数 ====================

def format_speed(speed):
    """格式化接口速率 (bps → 可读)"""
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
    except (ValueError, TypeError):
        return '未知'


def _parse_log_line(line):
    """
    解析 int_*.log 中的一行。
    支持格式: iso.3.6.1.2.1.2.2.1.2.1 = STRING: "InLoopBack0"

    返回: (ifIndex, oid_type, value)
      oid_type: 'name'(ifDescr .1.2) / 'speed'(ifSpeed .1.5) / 'status'(ifOperStatus .1.8)
    """
    m = re.match(
        r'(?:iso\.)?(\d+\.\d+\.\d+\.\d+\.\d+\.\d+\.\d+\.(\d+)\.(\d+))\s*=\s*\w+:\s*(.+)',
        line
    )
    if not m:
        return None

    oid_suffix = m.group(2)   # OID 字段编号: 2/5/8
    if_index = int(m.group(3))
    raw_value = m.group(4).strip()

    field_map = {'2': 'name', '5': 'speed', '8': 'status'}
    oid_type = field_map.get(oid_suffix)

    # 解析值: 去除引号和类型标记
    if oid_type == 'name':
        value = raw_value.strip('"').strip("'")
    elif oid_type == 'speed':
        value = re.sub(r'[^\d]', '', raw_value) or '0'
    elif oid_type == 'status':
        value = 'Up' if raw_value.strip() == '1' else 'Down'
    else:
        value = raw_value

    return (if_index, oid_type, value)


def load_ifindex_from_logs(log_dir=None):
    """
    解析 int_*.log 文件，提取已知的 ifIndex 列表。
    int_name.log  → ifDescr 数据
    int_speed.log → ifSpeed 数据
    int_status.log → ifOperStatus 数据

    返回: (if_index_set, interfaces_dict)
      interfaces_dict: {ifIndex: {name: ..., speed: ..., status: ...}}
    """
    if log_dir is None:
        log_dir = os.path.dirname(__file__)

    interfaces = {}  # {ifIndex: {name, speed, status}}

    for log_file in glob.glob(os.path.join(log_dir, 'int_*.log')):
        print(f"  [InterfaceMonitor] 解析日志: {os.path.basename(log_file)}")
        try:
            with open(log_file, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    parsed = _parse_log_line(line)
                    if parsed is None:
                        continue
                    if_index, oid_type, value = parsed
                    if if_index not in interfaces:
                        interfaces[if_index] = {'name': None, 'speed': None, 'status': None}
                    interfaces[if_index][oid_type] = value
        except Exception as e:
            print(f"  [InterfaceMonitor] 解析 {log_file} 出错: {e}")

    # 返回所有 ifIndex（即使某些字段缺失，后续 SNMP GET 会补齐）
    if_index_set = set(interfaces.keys())

    # 清理：只保留至少有 name 的接口数据作为缓存
    valid_for_cache = {}
    for idx, data in sorted(interfaces.items()):
        if data.get('name'):
            valid_for_cache[idx] = data

    return if_index_set, valid_for_cache


# ==================== SNMP GET（异步 → 同步包装）====================

async def _do_single_get(ip, community, oid, timeout=5, retries=1):
    """执行单次 SNMP GET，返回 (oid_str, raw_value_str) 或 (oid, None)"""
    last_err = None
    for attempt in range(retries + 1):
        try:
            errorIndication, errorStatus, errorIndex, varBinds = await asyncio.wait_for(
                get_cmd(
                    SnmpDispatcher(),
                    CommunityData(community),
                    await UdpTransportTarget.create((ip, 161), timeout=timeout, retries=0),
                    ObjectType(ObjectIdentity(oid))
                ),
                timeout=timeout + 2
            )
            if errorIndication:
                last_err = str(errorIndication)
                continue
            elif errorStatus:
                last_err = errorStatus.prettyPrint()
                continue
            else:
                for vb in varBinds:
                    return (str(vb[0]), str(vb[1]))
        except asyncio.TimeoutError:
            last_err = 'Timeout'
            continue
        except Exception as e:
            last_err = str(e)
            continue
    return (oid, None)


async def _batch_get(ip, community, oid_list, timeout=5, retries=1, concurrency=50):
    """
    批量并发 SNMP GET。
    通过 Semaphore 控制并发数，避免过多连接打爆设备。
    """
    sem = asyncio.Semaphore(concurrency)

    async def _get_with_sem(oid):
        async with sem:
            return await _do_single_get(ip, community, oid, timeout, retries)

    tasks = [_get_with_sem(oid) for oid in oid_list]
    return await asyncio.gather(*tasks)


def batch_snmp_get(ip, community, oid_list, timeout=5, retries=1, concurrency=50):
    """同步包装: 批量 SNMP GET"""
    return asyncio.run(_batch_get(ip, community, oid_list, timeout, retries, concurrency))


# ==================== 接口数据采集器 ====================

class InterfaceMonitor:
    """硬采集式接口数据采集器"""

    # OID 前缀（不含 ifIndex 后缀）
    IFDESCR_OID  = '1.3.6.1.2.1.2.2.1.2'   # 名称
    IFSPEED_OID  = '1.3.6.1.2.1.2.2.1.5'   # 速率
    IFSTATUS_OID = '1.3.6.1.2.1.2.2.1.8'   # 状态

    def __init__(self, ip='172.16.100.100', community='Public123'):
        self.ip = ip
        self.community = community

        # 缓存
        self._cached_interfaces = {}
        self._last_refresh = None
        self.REFRESH_INTERVAL = 300     # 秒
        self.GET_TIMEOUT = 5            # 单次 GET 超时（秒）
        self.GET_CONCURRENCY = 50       # 并发 GET 数

        # 线程
        self._thread = None

        # 统计
        self._collect_count = 0
        self._fail_count = 0
        self._last_duration = 0

        # ---- 从日志加载 ifIndex 索引 ----
        self._ifindex_set, fallback_data = load_ifindex_from_logs()
        print(f"[InterfaceMonitor] 从日志解析到 {len(self._ifindex_set)} 个接口索引")

        # 如果有完整的日志数据，直接作为初始缓存
        if fallback_data:
            self._build_cache_from_raw(fallback_data)
            if self._cached_interfaces:
                print(f"[InterfaceMonitor] 日志数据已加载: {len(self._cached_interfaces)} 个接口")
                self._last_refresh = datetime.now()

        if not self._ifindex_set:
            # 日志也没有 → 用硬编码的最小列表兜底
            print("[InterfaceMonitor] 警告: 未从日志解析到接口索引，使用兜底列表")
            self._ifindex_set = set(range(1, 470))

        print(f"[InterfaceMonitor] 初始化完成, 刷新间隔={self.REFRESH_INTERVAL}s, "
              f"并发={self.GET_CONCURRENCY}, 超时={self.GET_TIMEOUT}s")

    def _build_cache_from_raw(self, raw):
        """从 raw {ifIndex: {name, speed, status}} 构建缓存"""
        interfaces = {}
        for if_index, data in sorted(raw.items()):
            name = data.get('name')
            if not name:
                continue
            name = name.strip('"').strip("'")
            speed_str = data.get('speed', '0') or '0'
            status_str = data.get('status', 'Down') or 'Down'

            # 速度可能已经是格式化后的值（来自日志），也可能是原始数值
            try:
                # 如果 speed 是纯数字，按 bps 格式化
                int_val = int(speed_str)
                speed = format_speed(int_val)
            except ValueError:
                speed = speed_str

            interfaces[name] = {
                'status': 'Up' if status_str in ('Up', '1') else 'Down',
                'speed': speed,
                'if_index': if_index,
            }

        self._cached_interfaces = interfaces

    # ==================== 接口数据采集 ====================

    async def _walk_one_oid(self, oid, timeout=15):
        """
        手工 SNMP WALK（基于 next_cmd 逐条获取）。
        避免 GetBulk 兼容性问题，已在华为设备验证可行。
        """
        results = []
        current_oid = oid
        dispatcher = SnmpDispatcher()

        while True:
            try:
                errorIndication, errorStatus, _, varBinds = await asyncio.wait_for(
                    next_cmd(
                        dispatcher,
                        CommunityData(self.community),
                        await UdpTransportTarget.create((self.ip, 161), timeout=timeout, retries=1),
                        ObjectType(ObjectIdentity(current_oid))
                    ),
                    timeout=timeout + 10
                )
            except asyncio.TimeoutError:
                print(f"  [InterfaceMonitor] WALK 单步超时({timeout+10}s): {current_oid}，已获取 {len(results)} 条")
                return results

            if errorIndication:
                print(f"  [InterfaceMonitor] WALK错误: {errorIndication}")
                break
            elif errorStatus:
                print(f"  [InterfaceMonitor] WALK状态错误: {errorStatus.prettyPrint()}")
                break
            else:
                for vb in varBinds:
                    oid_str = str(vb[0])
                    val = str(vb[1])
                    # 判断是否走出了目标子树
                    if not oid_str.startswith(oid + '.'):
                        return results
                    results.append((oid_str, val))
                    current_oid = oid_str

                # 每 50 条打印一次进度
                if len(results) % 50 == 0:
                    print(f"  [InterfaceMonitor] WALK 进度: {oid} → {len(results)} 条")

        return results

    async def _walk_all_three(self, timeout=15):
        """串行 WALK 三个 OID，返回 {field: [(oid_str, value), ...]}。
        串行而非并发，避免大量 UDP 连接同时轰炸设备导致超时。"""
        result = {}
        for field, oid in [('name', self.IFDESCR_OID),
                           ('speed', self.IFSPEED_OID),
                           ('status', self.IFSTATUS_OID)]:
            print(f"  [InterfaceMonitor] WALK {field}: {oid}")
            try:
                result[field] = await self._walk_one_oid(oid, timeout)
            except Exception as e:
                print(f"  [InterfaceMonitor] WALK {field} 异常: {e}")
                result[field] = []
        return result

    def _collect_interface_data(self):
        """
        接口采集: 串行 WALK 名称/速率/状态三个 OID（基于 next_cmd）。
        串行避免并发 UDP 连接过多，每条 next_cmd 超时 25s。
        """
        start = time.time()
        print(f"\n[{datetime.now().strftime('%H:%M:%S')}] [InterfaceMonitor] "
              f"开始接口采集 (串行WALK)...")

        try:
            # ---- 步骤1: 串行 WALK 三个 OID ----
            walks = asyncio.run(self._walk_all_three(timeout=15))

            raw = {}  # {ifIndex: {name, speed, status}}
            total_fetched = 0

            for field, results in walks.items():
                if not results:
                    print(f"  [InterfaceMonitor] 警告: {field} WALK 无数据")
                    self._fail_count += 1
                    continue
                print(f"  [InterfaceMonitor] {field}: 获取到 {len(results)} 条")
                total_fetched += len(results)

                for oid_str, value in results:
                    try:
                        idx = int(oid_str.rsplit('.', 1)[-1])
                    except ValueError:
                        continue
                    if idx not in raw:
                        raw[idx] = {}
                    raw[idx][field] = value

            print(f"  [InterfaceMonitor] 共获取 {total_fetched} 条数据, "
                  f"覆盖 {len(raw)} 个接口索引")

            if not raw:
                print("  [InterfaceMonitor] 所有 WALK 均返回空数据")
                self._fail_count += 1
                if self._cached_interfaces:
                    age = (datetime.now() - self._last_refresh).total_seconds() if self._last_refresh else 0
                    print(f"  [InterfaceMonitor] 返回过期缓存 (已缓存 {age:.0f}秒)")
                    return self._cached_interfaces
                return {}

            # ---- 步骤2: 构建结构化接口信息 ----
            interfaces = {}
            for if_index in sorted(raw.keys()):
                data = raw[if_index]

                name = data.get('name', f'Interface-{if_index}')
                name = name.strip('"').strip("'")

                speed_raw = data.get('speed', '0')
                speed = format_speed(speed_raw)

                status_raw = data.get('status', '2')
                try:
                    status = 'Up' if int(status_raw) == 1 else 'Down'
                except (ValueError, TypeError):
                    status = 'Down'

                interfaces[name] = {
                    'status': status,
                    'speed': speed,
                    'if_index': if_index,
                }

            # ---- 步骤3: 更新缓存 ----
            self._cached_interfaces = interfaces
            self._last_refresh = datetime.now()
            self._collect_count += 1
            self._last_duration = round(time.time() - start, 2)
            self._ifindex_set = set(raw.keys())

            print(f"  [InterfaceMonitor] 采集完成: {len(interfaces)} 个接口, "
                  f"耗时={self._last_duration}s")

        except Exception as e:
            self._fail_count += 1
            print(f"  [InterfaceMonitor] 接口采集异常: {e}")
            import traceback
            traceback.print_exc()
            if self._cached_interfaces:
                print("  [InterfaceMonitor] 返回过期缓存")
                return self._cached_interfaces

        return interfaces

    # ==================== API 供外部调用 ====================

    def get_interfaces(self):
        """获取接口数据（仅返回缓存）。"""
        return dict(self._cached_interfaces)

    def get_last_refresh(self):
        """返回上次刷新时间字符串"""
        if self._last_refresh:
            return self._last_refresh.strftime('%Y-%m-%d %H:%M:%S')
        return None

    def get_stats(self):
        """返回采集统计信息"""
        return {
            'collect_count': self._collect_count,
            'fail_count': self._fail_count,
            'last_duration': self._last_duration,
            'cached_interface_count': len(self._cached_interfaces),
            'ifindex_count': len(self._ifindex_set),
            'last_refresh': self.get_last_refresh(),
        }

    # ==================== 后台采集线程 ====================

    def _run_loop(self):
        """后台采集循环"""
        print("[InterfaceMonitor] 首次硬采集接口数据...")
        self._collect_interface_data()

        while True:
            try:
                time.sleep(self.REFRESH_INTERVAL)
                self._collect_interface_data()
            except Exception as e:
                print(f"[InterfaceMonitor] 采集循环异常: {e}")
                time.sleep(10)

    def start(self):
        """启动接口数据采集线程"""
        if self._thread is not None and self._thread.is_alive():
            print("[InterfaceMonitor] 采集线程已在运行")
            return self._thread

        self._thread = threading.Thread(
            target=self._run_loop,
            daemon=True,
            name="InterfaceMonitor"
        )
        self._thread.start()
        print(f"[InterfaceMonitor] 采集线程已启动 (刷新间隔={self.REFRESH_INTERVAL}s)")
        return self._thread


# ==================== 全局实例 ====================

interface_monitor = InterfaceMonitor()


def start_interface_monitoring():
    """启动接口数据采集（供 web.py 调用）"""
    return interface_monitor.start()


# ==================== 独立测试入口 ====================

if __name__ == "__main__":
    print("=" * 50)
    print("接口数据采集模块 - 硬采集测试")
    print("=" * 50)

    # 先显示从日志解析到的数据
    if interface_monitor._cached_interfaces:
        print(f"\n日志预加载: {len(interface_monitor._cached_interfaces)} 个接口")
        for name, info in list(interface_monitor._cached_interfaces.items())[:10]:
            print(f"  {name}: status={info['status']}, speed={info['speed']}, "
                  f"index={info['if_index']}")

    # 启动采集线程
    interface_monitor.start()

    print("\n等待首次 SNMP 硬采集完成...")
    while interface_monitor.get_last_refresh() is None:
        time.sleep(1)

    interfaces = interface_monitor.get_interfaces()
    print(f"\n采集完成: {len(interfaces)} 个接口")
    for name, info in list(interfaces.items())[:10]:
        print(f"  {name}: status={info['status']}, speed={info['speed']}, "
              f"index={info['if_index']}")

    stats = interface_monitor.get_stats()
    print(f"\n统计: {stats}")
    print("\n测试完成！采集线程在后台继续运行...")
    print("按 Ctrl+C 退出")
