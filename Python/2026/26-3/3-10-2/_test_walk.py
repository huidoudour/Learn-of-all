from interface import snmp_walk
import sys
print('Testing WALK...', flush=True)
try:
    results = snmp_walk('172.16.100.100', 'Public123', '1.3.6.1.2.1.2.2.1.2', timeout=8)
    print(f'Results: {len(results)} items', flush=True)
    if results:
        for r in results[:5]:
            print(f'  {r[0]} = {r[1]}', flush=True)
    else:
        print('  EMPTY!', flush=True)
except Exception as e:
    print(f'ERROR: {e}', flush=True)
    import traceback
    traceback.print_exc()
