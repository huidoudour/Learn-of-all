import asyncio
from pysnmp.hlapi.v1arch.asyncio import (
    get_cmd, SnmpDispatcher,
    CommunityData, UdpTransportTarget, ObjectType, ObjectIdentity
)

async def test_get(oid, label):
    try:
        errorIndication, errorStatus, errorIndex, varBinds = await asyncio.wait_for(
            get_cmd(
                SnmpDispatcher(),
                CommunityData('Public123'),
                await UdpTransportTarget.create(('172.16.100.100', 161), timeout=5, retries=1),
                ObjectType(ObjectIdentity(oid))
            ),
            timeout=8
        )
        if errorIndication:
            print(f"{label}: 错误 - {errorIndication}")
        elif errorStatus:
            print(f"{label}: 状态错误 - {errorStatus.prettyPrint()}")
        else:
            for vb in varBinds:
                print(f"{label}: {vb[0]} = {vb[1]}")
    except asyncio.TimeoutError:
        print(f"{label}: 超时")
    except Exception as e:
        print(f"{label}: 异常 - {e}")

async def main():
    # Test: sysDescr (known to work from monitor.py)
    await test_get('1.3.6.1.2.1.1.1.0', 'sysDescr')
    # Test: ifDescr.1
    await test_get('1.3.6.1.2.1.2.2.1.2.1', 'ifDescr.1')
    # Test: ifSpeed.1
    await test_get('1.3.6.1.2.1.2.2.1.5.1', 'ifSpeed.1')

asyncio.run(main())
