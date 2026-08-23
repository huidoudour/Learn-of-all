#!/usr/bin/env python3
"""
SNMP 多 OID 交错输出工具
使用 pysnmp 库实现，无需依赖外部 snmpwalk 命令
"""

import asyncio
from typing import List, Tuple, Optional

from pysnmp.hlapi.v1arch.asyncio import SnmpDispatcher, CommunityData, UdpTransportTarget
from pysnmp.hlapi.v1arch.asyncio.cmdgen import walk_cmd
from pysnmp.smi.rfc1902 import ObjectType, ObjectIdentity


async def snmp_walk(host: str, community: str, oid: str) -> List[Tuple[str, str]]:
    """
    执行 SNMP walk 操作

    Args:
        host: 目标设备 IP
        community: community 字符串
        oid: 起始 OID

    Returns:
        List of (oid, value) 元组
    """
    results = []

    try:
        # 创建 SNMP 调度器
        dispatcher = SnmpDispatcher()

        # 创建传输目标
        transport_target = await UdpTransportTarget.create(
            (host, 161),
            timeout=3,
            retries=2
        )

        # 创建 SNMP walk 迭代器
        async for errorIndication, errorStatus, errorIndex, varBinds in walk_cmd(
            dispatcher,
            CommunityData(community, mpModel=1),  # mpModel=1 表示 SNMP v2c
            transport_target,
            ObjectType(ObjectIdentity(oid))
        ):
            if errorIndication:
                print(f"错误: {errorIndication}")
                break
            elif errorStatus:
                print(f"错误: {errorStatus}")
                break
            else:
                for varBind in varBinds:
                    oid_str = varBind[0].prettyPrint()
                    value_str = varBind[1].prettyPrint()
                    results.append((oid_str, value_str))

    except Exception as e:
        print(f"发生异常: {e}")
    finally:
        # 关闭调度器
        if 'dispatcher' in locals():
            dispatcher.close_dispatcher()

    return results


def extract_interface_index(oid: str) -> Optional[int]:
    """
    从 OID 中提取接口索引
    例如: 1.3.6.1.2.1.2.2.1.2.1 -> 1
    """
    parts = oid.split('.')
    if len(parts) >= 12:
        try:
            return int(parts[-1])
        except ValueError:
            return None
    return None


def merge_by_interface_index(
    results1: List[Tuple[str, str]],
    results2: List[Tuple[str, str]],
    results3: List[Tuple[str, str]]
) -> List[dict]:
    """
    按接口索引合并三个 OID 的结果
    """
    # 转换为字典，key 为接口索引
    dict1 = {extract_interface_index(oid): value for oid, value in results1}
    dict2 = {extract_interface_index(oid): value for oid, value in results2}
    dict3 = {extract_interface_index(oid): value for oid, value in results3}

    # 获取所有接口索引
    all_indices = set(dict1.keys()) | set(dict2.keys()) | set(dict3.keys())
    # 过滤掉 None 并按数字排序
    all_indices = sorted([i for i in all_indices if i is not None])

    # 构建合并结果
    merged = []
    for idx in all_indices:
        merged.append({
            'interface_index': idx,
            'ifDescr': dict1.get(idx, 'N/A'),
            'ifSpeed': dict2.get(idx, 'N/A'),
            'ifOperStatus': dict3.get(idx, 'N/A')
        })

    return merged


async def main():
    # 配置参数
    HOST = '172.16.100.100'
    COMMUNITY = 'Public123'

    # 定义要查询的 OID 及其描述
    oids = {
        'ifDescr': '1.3.6.1.2.1.2.2.1.2',      # 接口描述
        'ifSpeed': '1.3.6.1.2.1.2.2.1.5',      # 接口速度
        'ifOperStatus': '1.3.6.1.2.1.2.2.1.8'  # 接口操作状态
    }

    print(f"正在从 {HOST} 获取 SNMP 数据...")
    print("-" * 60)

    # 执行三个 walk 操作
    results = {}
    for name, oid in oids.items():
        print(f"正在获取 {name}...")
        results[name] = await snmp_walk(HOST, COMMUNITY, oid)

    # 按接口索引合并结果
    merged = merge_by_interface_index(
        results['ifDescr'],
        results['ifSpeed'],
        results['ifOperStatus']
    )

    # 交错输出
    print("\n" + "=" * 60)
    print("交错输出结果")
    print("=" * 60)

    for item in merged:
        idx = item['interface_index']
        print(f"\n=== 命令1 (ifDescr) - 接口 {idx} ===")
        print(f"  {item['ifDescr']}")

        print(f"=== 命令2 (ifSpeed) - 接口 {idx} ===")
        print(f"  {item['ifSpeed']}")

        print(f"=== 命令3 (ifOperStatus) - 接口 {idx} ===")
        # 将状态码转换为可读文本
        status = item['ifOperStatus']
        status_text = {
            '1': 'up',
            '2': 'down',
            '3': 'testing',
            '4': 'unknown',
            '5': 'dormant',
            '6': 'notPresent',
            '7': 'lowerLayerDown'
        }.get(status, status)
        print(f"  {status_text}")
        print("-" * 40)


if __name__ == '__main__':
    asyncio.run(main())