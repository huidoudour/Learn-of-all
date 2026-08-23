import os
from dotenv import load_dotenv
from email_notifier import send_version_notification

load_dotenv()

print("=== 2980 邮箱服务测试 ===")
print(f"SMTP 服务器: {os.getenv('SMTP_SERVER')}")
print(f"SMTP 端口: {os.getenv('SMTP_PORT')}")
print(f"发送邮箱: {os.getenv('EMAIL_SENDER')}")
print(f"接收邮箱: {os.getenv('EMAIL_RECEIVER')}")
print(f"授权码: {'*' * len(os.getenv('EMAIL_AUTH_CODE', ''))}")
print()

success = send_version_notification(
    name="版本更新提醒",
    old_version="1.0.0",
    new_version="1.1.0",
    url="https://gradle.org/releases/",
)

if success:
    print("\n测试成功！邮件已发送。")
else:
    print("\n测试失败，请检查 .env 配置和网络连接。")