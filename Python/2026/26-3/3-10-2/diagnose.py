#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SNMP连接诊断工具
用于测试SNMP连接和OID信息
"""

import asyncio
from pysnmp.hlapi.v1arch.asyncio import *
from ping3 import ping

def test_ping(ip):
    """测试网络连通性"""
    print(f"测试 {ip} 的网络连通性...")
    try:
        response_time = ping(ip, timeout=2)
        if response_time is not None:
            print(f" Ping成功，响应时间: {round(response_time * 1000, 2)}ms")
            return True
        else:
            print(f" Ping失败，设备可能离线")
            return False
    except Exception as e:
        print(f" Ping错误: {e}")
        return False

async def test_snmp_get(ip, community, oid):
    """测试SNMP GET操作"""
    try:
        errorIndication, errorStatus, errorIndex, varBinds = await get_cmd(
            SnmpDispatcher(),
            CommunityData(community),
            await UdpTransportTarget.create((ip, 161)),
            ObjectType(ObjectIdentity(oid))
        )
        
        if errorIndication:
            print(f" SNMP错误: {errorIndication}")
            return None
        elif errorStatus:
            print(f" SNMP状态错误: {errorStatus.prettyPrint()}")
            return None
        else:
            for varBind in varBinds:
                value = str(varBind[1])
                print(f" SNMP GET成功: {oid} = {value}")
                return value
    except Exception as e:
        print(f" SNMP操作错误: {e}")
        return None

def test_snmp(ip, community):
    """测试SNMP连接"""
    print(f"测试 {ip} 的SNMP连接...")
    
    # 测试系统描述
    sys_descr = asyncio.run(test_snmp_get(ip, community, '1.3.6.1.2.1.1.1.0'))
    if sys_descr:
        print(f"设备描述: {sys_descr}")
        return True
    else:
        return False

def discover_oids(ip, community):
    """发现常用OID"""
    print(f"发现 {ip} 的常用OID...")
    
    common_oids = {
        '系统描述': '1.3.6.1.2.1.1.1.0',
        '设备名称': '1.3.6.1.2.1.1.5.0',
        '运行时间': '1.3.6.1.2.1.1.3.0',
        '接口数量': '1.3.6.1.2.1.2.1.0',
        'IP接收包数': '1.3.6.1.2.1.4.3.0',
        'IP发送包数': '1.3.6.1.2.1.4.10.0'
    }
    
    results = {}
    for name, oid in common_oids.items():
        value = asyncio.run(test_snmp_get(ip, community, oid))
        if value:
            results[name] = value
    
    return results

def main():
    """主函数"""
    print("===============================================")
    print("SNMP连接诊断工具")
    print("===============================================")
    
    ip = '172.16.100.100'
    community = 'Public123'
    
    print(f"测试设备: {ip}")
    print(f"SNMP团体字: {community}")
    print("===============================================")
    
    # 测试网络连通性
    if not test_ping(ip):
        print(" 网络连通性测试失败，无法继续")
        return
    
    # 测试SNMP连接
    if not test_snmp(ip, community):
        print(" SNMP连接测试失败")
        return
    
    # 发现常用OID
    print("===============================================")
    print("发现常用OID:")
    results = discover_oids(ip, community)
    
    if results:
        print("===============================================")
        print("发现的OID信息:")
        for name, value in results.items():
            print(f"{name}: {value}")
    else:
        print(" 未发现任何OID信息")
    
    print("===============================================")
    print("诊断完成！")

if __name__ == "__main__":
    main()
