#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
简单SNMP测试
"""

import asyncio
from pysnmp.hlapi.v1arch.asyncio import *
from ping3 import ping

async def test_snmp():
    """测试SNMP连接"""
    print(" 测试Ping连接...")
    try:
        response = ping('192.168.8.1', timeout=2)
        if response is not None:
            print(f" Ping成功 - 响应时间: {round(response * 1000, 2)}ms")
        else:
            print(" Ping失败")
            return
    except Exception as e:
        print(f" Ping错误: {e}")
        return
    
    print("\n 测试SNMP连接...")
    
    # 测试系统描述
    try:
        errorIndication, errorStatus, errorIndex, varBinds = await get_cmd(
            SnmpDispatcher(),
            CommunityData('Public123'),
            await UdpTransportTarget.create(('192.168.8.1', 161)),
            ObjectType(ObjectIdentity('1.3.6.1.2.1.1.1.0'))
        )
        
        if errorIndication:
            print(f" SNMP错误: {errorIndication}")
        elif errorStatus:
            print(f" SNMP状态错误")
        else:
            for varBind in varBinds:
                print(f" SNMP成功 - 系统描述: {str(varBind[1])}")
                
    except Exception as e:
        print(f" SNMP异常: {e}")
        import traceback
        traceback.print_exc()

def test_basic():
    """基础测试"""
    asyncio.run(test_snmp())

if __name__ == "__main__":
    test_basic()
