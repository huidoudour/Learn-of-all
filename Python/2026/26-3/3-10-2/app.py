#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SNMP网络设备监控Web应用
基于Flask的Web界面，提供实时监控数据展示
"""

from flask import Flask, render_template, jsonify, request
import json
import time
from datetime import datetime
from snmp_monitor import monitor, start_monitoring

app = Flask(__name__)

# 启动监控线程
monitoring_thread = start_monitoring()

@app.route('/')
def index():
    """主页面"""
    return render_template('index.html')

@app.route('/api/current_data')
def get_current_data():
    """获取当前监控数据"""
    try:
        data = monitor.collect_all_data()
        return jsonify({
            'success': True,
            'data': data,
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        })

@app.route('/api/history_data')
def get_history_data():
    """获取历史数据用于图表显示"""
    try:
        # 检查是否需要显示所有历史数据
        show_all = request.args.get('show_all', 'false').lower() == 'true'
        history = monitor.get_history_data(show_all=show_all)
        
        # 格式化历史数据用于图表
        formatted_history = {}
        
        # CPU使用率历史
        formatted_history['cpu'] = [
            {
                'time': item['time'].strftime('%H:%M:%S'),
                'usage': item['usage']
            }
            for item in history['cpu_usage']
        ]
        
        # 内存使用率历史
        formatted_history['memory'] = [
            {
                'time': item['time'].strftime('%H:%M:%S'),
                'usage': item['usage']
            }
            for item in history['memory_usage']
        ]
        
        # 包传输速率历史
        formatted_history['packets'] = [
            {
                'time': item['time'].strftime('%H:%M:%S'),
                'in_packets': item['in_packets'],
                'out_packets': item['out_packets']
            }
            for item in history['packet_rates']
        ]
        
        # Ping状态历史
        formatted_history['ping'] = [
            {
                'time': item['time'].strftime('%H:%M:%S'),
                'status': item['status'],
                'response_time': item['response_time']
            }
            for item in history['ping_status']
        ]
        
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
        })

@app.route('/api/device_info')
def get_device_info():
    """获取设备详细信息"""
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
        })

@app.route('/api/interfaces')
def get_interfaces():
    """获取接口详细信息"""
    try:
        interfaces = monitor.get_interface_status()
        return jsonify({
            'success': True,
            'interfaces': interfaces,
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        })

@app.route('/api/ping_test')
def ping_test():
    """执行Ping测试"""
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
        })

@app.route('/api/refresh')
def refresh_data():
    """手动刷新数据"""
    try:
        data = monitor.collect_all_data()
        return jsonify({
            'success': True,
            'data': data,
            'message': '数据刷新成功',
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        })

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
        })

if __name__ == '__main__':
    print("启动SNMP网络设备监控Web应用...")
    print("监控设备: 172.16.100.100")
    print("SNMP团体字: Public123")
    print("Web界面地址: http://localhost:5000")
    
    app.run(host='0.0.0.0', port=5000, debug=True)
