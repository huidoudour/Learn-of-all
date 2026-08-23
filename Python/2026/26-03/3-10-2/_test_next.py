import asyncio
from pysnmp.hlapi.v1arch.asyncio import (
    next_cmd, SnmpDispatcher,
    CommunityData, UdpTransportTarget, ObjectType, ObjectIdentity
)

async def manual_walk(oid):
    results = []
    current_oid = oid
    while True:
        try:
            errorIndication, errorStatus, errorIndex, varBinds = await asyncio.wait_for(
                next_cmd(
                    SnmpDispatcher(),
                    CommunityData('Public123'),
                    await UdpTransportTarget.create(('172.16.100.100', 161), timeout=5, retries=1),
                    ObjectType(ObjectIdentity(current_oid))
                ),
                timeout=8
            )
            if errorIndication:
                print(f"  错误: {errorIndication}")
                break
            elif errorStatus:
                print(f"  状态错误: {errorStatus.prettyPrint()}")
                break
            else:
                for vb in varBinds:
                    oid_str = str(vb[0])
                    val = str(vb[1])
                    # Check if we've gone past the subtree
                    if not oid_str.startswith(oid + '.'):
                        return results
                    results.append((oid_str, val))
                    current_oid = oid_str
        except asyncio.TimeoutError:
            print("  超时")
            break
        except Exception as e:
            print(f"  异常: {e}")
            break
    return results

async def main():
    print("Manual GET-NEXT walk on ifDescr...")
    results = await manual_walk('1.3.6.1.2.1.2.2.1.2')
    print(f"Got {len(results)} interfaces")
    for r in results[:5]:
        print(f"  {r[0]} = {r[1]}")

asyncio.run(main())
