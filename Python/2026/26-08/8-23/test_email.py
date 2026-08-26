import os
from dotenv import load_dotenv
from email_notifier import send_version_notification
from log import log

load_dotenv()

log("=== 邮箱服务测试 ===")
log(f"SMTP 服务器: {os.getenv('SMTP_SERVER')}")
log(f"SMTP 端口: {os.getenv('SMTP_PORT')}")
log(f"发送邮箱: {os.getenv('EMAIL_SENDER')}")
log(f"接收邮箱: {os.getenv('EMAIL_RECEIVER')}")
log(f"抄送邮箱: {os.getenv('EMAIL_CC')}")
log(f"授权码: {'*' * len(os.getenv('EMAIL_AUTH_CODE', ''))}")

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