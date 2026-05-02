#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SNMP监控系统诊断工具
检查SNMP连接和OID配置问题
"""

import subprocess
import sys
from pysnmp.hlapi import getCmd, nextCmd, CommunityData, UdpTransportTarget, ContextData, ObjectType, ObjectIdentity
from ping3 import ping

def test_ping():
    """测试Ping连接"""
    print(" 测试Ping连接...")
    try:
        response = ping('192.168.8.1', timeout=2)
        if response is not None:
            print(f" Ping成功 - 响应时间: {round(response * 1000, 2)}ms")
            return True
        else:
            print(" Ping失败 - 设备无响应")
            return False
    except Exception as e:
        print(f" Ping错误: {e}")
        return False

def test_snmp_connection():
    """测试SNMP基础连接"""
    print("\n 测试SNMP基础连接...")
    
    # 测试系统描述OID
    oid = '1.3.6.1.2.1.1.1.0'
    
    try:
        errorIndication, errorStatus, errorIndex, varBinds = next(
            getCmd(SnmpEngine(),
                   CommunityData('Public123'),
                   UdpTransportTarget(('192.168.8.1', 161)),
                   ContextData(),
                   ObjectType(ObjectIdentity(oid)))
        )
        
        if errorIndication:
            print(f" SNMP错误: {errorIndication}")
            return False
        elif errorStatus:
            print(f" SNMP状态错误: {errorStatus.prettyPrint()} at {errorIndex and varBinds[int(errorIndex)-1][0] or '?'}")
            return False
        else:
            for varBind in varBinds:
                print(f" SNMP连接成功")
                print(f"   系统描述: {str(varBind[1])}")
                return True
                
    except Exception as e:
        print(f" SNMP连接异常: {e}")
        return False

def test_common_oids():
    """测试常用OID"""
    print("\n 测试常用OID...")
    
    # 常用OID列表
    oids = {
        '系统描述': '1.3.6.1.2.1.1.1.0',
        '设备名称': '1.3.6.1.2.1.1.5.0',
        '运行时间': '1.3.6.1.2.1.1.3.0',
        '系统联系人': '1.3.6.1.2.1.1.4.0',
        '系统位置': '1.3.6.1.2.1.1.6.0',
        '接口数量': '1.3.6.1.2.1.2.1.0'
    }
    
    for name, oid in oids.items():
        try:
            errorIndication, errorStatus, errorIndex, varBinds = next(
                getCmd(SnmpEngine(),
                       CommunityData('Public123'),
                       UdpTransportTarget(('192.168.8.1', 161)),
                       ContextData(),
                       ObjectType(ObjectIdentity(oid)))
            )
            
            if errorIndication:
                print(f"    {name}: 错误 - {errorIndication}")
            elif errorStatus:
                print(f"    {name}: 状态错误")
            else:
                for varBind in varBinds:
                    value = str(varBind[1])
                    print(f"    {name}: {value}")
                    
        except Exception as e:
            print(f"    {name}: 异常 - {e}")

def test_huawei_specific_oids():
    """测试华为设备专用OID"""
    print("\n 测试华为设备专用OID...")
    
    # 华为设备OID
    huawei_oids = {
        'CPU使用率': '1.3.6.1.4.1.2011.6.3.4.1.2.0',
        '内存总量': '1.3.6.1.4.1.2011.6.3.5.1.1.2.0',
        '内存使用': '1.3.6.1.4.1.2011.6.3.5.1.1.3.0',
        '接收包数': '1.3.6.1.2.1.4.3.0',
        '发送包数': '1.3.6.1.2.1.4.10.0'
    }
    
    for name, oid in huawei_oids.items():
        try:
            errorIndication, errorStatus, errorIndex, varBinds = next(
                getCmd(SnmpEngine(),
                       CommunityData('Public123'),
                       UdpTransportTarget(('192.168.8.1', 161)),
                       ContextData(),
                       ObjectType(ObjectIdentity(oid)))
            )
            
            if errorIndication:
                print(f"    {name}: 错误 - {errorIndication}")
            elif errorStatus:
                print(f"    {name}: 状态错误")
            else:
                for varBind in varBinds:
                    value = str(varBind[1])
                    print(f"    {name}: {value}")
                    
        except Exception as e:
            print(f"    {name}: 异常 - {e}")

def test_snmp_walk():
    """测试SNMP WALK操作"""
    print("\n 测试SNMP WALK操作...")
    
    # 测试接口名称
    try:
        results = []
        for (errorIndication, errorStatus, errorIndex, varBinds) in nextCmd(
            SnmpEngine(),
            CommunityData('Public123'),
            UdpTransportTarget(('192.168.8.1', 161)),
            ContextData(),
            ObjectType(ObjectIdentity('1.3.6.1.2.1.2.2.1.2')),
            lexicographicMode=False
        ):
            if errorIndication:
                print(f"    WALK错误: {errorIndication}")
                break
            elif errorStatus:
                print(f"    WALK状态错误")
                break
            else:
                for varBind in varBinds:
                    results.append((str(varBind[0]), str(varBind[1])))
        
        if results:
            print(f"    发现 {len(results)} 个接口")
            for i, (oid, name) in enumerate(results[:5]):  # 只显示前5个
                print(f"      接口{i+1}: {name}")
            if len(results) > 5:
                print(f"      ... 还有 {len(results)-5} 个接口")
        else:
            print("    未发现接口")
            
    except Exception as e:
        print(f"    WALK异常: {e}")

def check_firewall():
    """检查防火墙设置"""
    print("\n 检查防火墙设置...")
    
    try:
        # 检查Windows防火墙
        result = subprocess.run(['netsh', 'advfirewall', 'show', 'allprofiles'], 
                              capture_output=True, text=True)
        
        if 'ON' in result.stdout:
            print("️  防火墙已启用，请确保SNMP端口(161)和Web端口(5000)已放行")
        else:
            print(" 防火墙状态正常")
            
    except Exception as e:
        print(f"️  无法检查防火墙状态: {e}")

def main():
    """主诊断函数"""
    print(" SNMP监控系统诊断工具")
    print("=" * 50)
    
    # 执行各项测试
    ping_ok = test_ping()
    snmp_ok = test_snmp_connection()
    
    if ping_ok and snmp_ok:
        test_common_oids()
        test_huawei_specific_oids()
        test_snmp_walk()
    
    check_firewall()
    
    print("\n" + "=" * 50)
    print(" 诊断结果汇总:")
    
    if ping_ok:
        print(" 网络连接正常")
    else:
        print(" 网络连接失败")
        print("   请检查设备192.168.8.1是否在线")
    
    if snmp_ok:
        print(" SNMP基础连接正常")
    else:
        print(" SNMP连接失败")
        print("   请检查:")
        print("   1. SNMP服务是否在设备上启用")
        print("   2. SNMP团体字是否正确(Public123)")
        print("   3. 防火墙是否阻止SNMP流量")
    
    print("\n 建议:")
    print("   如果华为专用OID无法获取，可能需要使用标准OID")
    print("   或者检查设备的具体OID映射")

if __name__ == "__main__":
    main()
