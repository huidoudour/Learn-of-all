#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SNMP网络设备监控Web应用
基于Flask的Web界面，提供实时监控数据展示。

数据来源：
- monitor.py   → CPU / 内存 / 包发送 / 设备信息（60秒刷新）
- interface.py → 接口数据（300秒刷新，SNMP WALK）

两个采集模块独立运行在不同线程，互不阻塞。
接口WALK耗时长（数十条~数百条），单独线程避免影响常规数据采集。
"""

from datetime import datetime

from flask import Flask, render_template, jsonify, request

# ---- 两个独立采集模块 ----
from monitor import monitor, start_monitoring
from interface import interface_monitor, start_interface_monitoring

app = Flask(__name__)

# ---- 启动双线程采集 ----
monitor_thread = start_monitoring()              # 常规数据: 60s
interface_thread = start_interface_monitoring()  # 接口数据: 300s


# ==================== 页面路由 ====================

@app.route('/')
def index():
    """主页面"""
    return render_template('index.html')


# ==================== 数据 API ====================

@app.route('/api/current_data')
def get_current_data():
    """
    获取当前所有监控数据。
    常规数据来自 monitor 缓存（不触发 SNMP GET）。
    接口数据来自 interface 缓存（不触发 SNMP WALK）。
    """
    try:
        general_data = monitor.get_cached_data()
        interfaces = interface_monitor.get_interfaces()

        # 合并两个模块的数据，保持向前兼容
        data = {**general_data, 'interfaces': interfaces}

        return jsonify({
            'success': True,
            'data': data,
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'general_refresh': monitor.get_last_refresh(),
            'interfaces_refresh': interface_monitor.get_last_refresh(),
            'interface_interval': interface_monitor.REFRESH_INTERVAL,
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }), 500


@app.route('/api/history_data')
def get_history_data():
    """获取历史数据用于图表显示"""
    try:
        show_all = request.args.get('show_all', 'false').lower() == 'true'
        history = monitor.get_history_data(show_all=show_all)

        formatted_history = {
            'cpu': [
                {'time': item['time'].strftime('%H:%M:%S'), 'usage': item['usage']}
                for item in history['cpu_usage']
            ],
            'memory': [
                {'time': item['time'].strftime('%H:%M:%S'), 'usage': item['usage']}
                for item in history['memory_usage']
            ],
            'packets': [
                {
                    'time': item['time'].strftime('%H:%M:%S'),
                    'in_packets': item['in_packets'],
                    'out_packets': item['out_packets']
                }
                for item in history['packet_rates']
            ],
            'ping': [
                {
                    'time': item['time'].strftime('%H:%M:%S'),
                    'status': item['status'],
                    'response_time': item['response_time']
                }
                for item in history['ping_status']
            ],
        }

        return jsonify({
            'success': True,
            'history': formatted_history,
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'show_all': show_all
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }), 500


@app.route('/api/device_info')
def get_device_info():
    """获取设备详细信息（实时 SNMP GET）"""
    try:
        info = monitor.get_device_info()
        return jsonify({
            'success': True,
            'info': info,
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }), 500


@app.route('/api/interfaces')
def get_interfaces():
    """获取接口详细信息（来自缓存，不触发 WALK）"""
    try:
        interfaces = interface_monitor.get_interfaces()
        return jsonify({
            'success': True,
            'interfaces': interfaces,
            'interface_count': len(interfaces),
            'last_refresh': interface_monitor.get_last_refresh(),
            'stats': interface_monitor.get_stats(),
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }), 500


@app.route('/api/ping_test')
def ping_test():
    """执行 Ping 测试"""
    try:
        result = monitor.ping_test()
        return jsonify({
            'success': True,
            'ping': result,
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }), 500


@app.route('/api/refresh')
def refresh_data():
    """手动刷新数据（返回缓存，不触发采集）"""
    try:
        general_data = monitor.get_cached_data()
        interfaces = interface_monitor.get_interfaces()
        data = {**general_data, 'interfaces': interfaces}

        return jsonify({
            'success': True,
            'data': data,
            'message': '数据刷新成功（来自缓存）',
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }), 500


@app.route('/api/clear_history')
def clear_history():
    """清除历史数据"""
    try:
        monitor.clear_history()
        return jsonify({
            'success': True,
            'message': '历史数据已清除',
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }), 500


@app.route('/api/stats')
def get_stats():
    """获取采集模块运行统计"""
    return jsonify({
        'success': True,
        'general': {
            'last_refresh': monitor.get_last_refresh(),
            'has_cache': bool(monitor._cached_general_data),
        },
        'interface': interface_monitor.get_stats(),
        'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
    })


# ==================== 启动入口 ====================

if __name__ == '__main__':
    print("=" * 55)
    print("SNMP网络设备监控Web应用")
    print("=" * 55)
    print(f"  监控设备: 172.16.100.100")
    print(f"  SNMP团体字: Public123")
    print(f"  常规数据采集: 60s 间隔 (monitor.py)")
    print(f"  接口数据采集: 300s 间隔 (interface.py)")
    print(f"  Web界面: http://localhost:5000")
    print("=" * 55)

    app.run(host='0.0.0.0', port=5000, debug=True)
