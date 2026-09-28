import os
from dotenv import load_dotenv
from email_notifier import send_version_notification, send_batch_notification
from log import log

load_dotenv()

log("=== 邮箱服务测试 ===")
log(f"SMTP 服务器: {os.getenv('SMTP_SERVER')}")
log(f"SMTP 端口: {os.getenv('SMTP_PORT')}")
log(f"发送邮箱: {os.getenv('EMAIL_SENDER')}")
log(f"接收邮箱: {os.getenv('EMAIL_RECEIVER')}")
log(f"抄送邮箱: {os.getenv('EMAIL_CC')}")
log(f"授权码: {'*' * len(os.getenv('EMAIL_AUTH_CODE', ''))}")

print("请选择要测试的通知类型：")
print("  1. 单邮件（单个依赖更新通知）")
print("  2. 多邮件（同一轮多个依赖更新，合并为一封）")
choice = input("请输入 1 或 2 (默认 1): ").strip()

if choice == "2":
    updates = [
        {
            "name": "Compose UI Tooling Preview",
            "old_version": "1.7.5",
            "new_version": "1.8.0",
            "url": "https://mvnrepository.com/artifact/androidx.compose.ui/ui-tooling-preview",
        },
        {
            "name": "Gradle",
            "old_version": "8.10.2",
            "new_version": "8.11.1",
            "url": "https://gradle.org/releases/",
        },
        {
            "name": "AndroidX SQLite",
            "old_version": "2.4.0",
            "new_version": "2.5.0",
            "url": "https://mvnrepository.com/artifact/androidx.sqlite/sqlite",
        },
    ]
    success = send_batch_notification(updates)
else:
    success = send_version_notification(
        name="邮件通知发送测试",
        old_version="1.0.0",
        new_version="1.1.0",
        url="https://www.baidu.com",
    )

if success:
    log("测试成功！邮件已发送。")
else:
    log("测试失败，请检查 .env 配置和网络连接。")