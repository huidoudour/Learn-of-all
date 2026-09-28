import os
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from email.header import Header
from dotenv import load_dotenv
from log import log

load_dotenv()

# 全部配置仅从 .env 读取，不再设置硬编码默认值
SMTP_SERVER = os.getenv("SMTP_SERVER", "")
SMTP_PORT = os.getenv("SMTP_PORT", "")
EMAIL_SENDER = os.getenv("EMAIL_SENDER", "")
EMAIL_AUTH_CODE = os.getenv("EMAIL_AUTH_CODE", "")
EMAIL_RECEIVER = os.getenv("EMAIL_RECEIVER", "")
EMAIL_CC = os.getenv("EMAIL_CC", "")


def parse_emails(email_str: str) -> list:
    if not email_str:
        return []
    return [e.strip() for e in email_str.split(",") if e.strip()]


def _build_and_send(subject: str, body: str):
    """构造并发送 HTML 邮件，复用 SMTP 连接逻辑。"""
    receivers = parse_emails(EMAIL_RECEIVER)
    cc_list = parse_emails(EMAIL_CC)
    all_recipients = receivers + cc_list

    if not all([SMTP_SERVER, SMTP_PORT, EMAIL_SENDER, EMAIL_AUTH_CODE, receivers]):
        log("[邮件] 配置不完整，跳过发送")
        return False

    msg = MIMEMultipart("alternative")
    msg["From"] = Header(EMAIL_SENDER)
    msg["To"] = Header(", ".join(receivers))
    if cc_list:
        msg["Cc"] = Header(", ".join(cc_list))
    msg["Subject"] = Header(subject, "utf-8")
    msg.attach(MIMEText(body, "html", "utf-8"))

    try:
        server = smtplib.SMTP_SSL(SMTP_SERVER, int(SMTP_PORT))
        server.login(EMAIL_SENDER, EMAIL_AUTH_CODE)
        server.sendmail(EMAIL_SENDER, all_recipients, msg.as_string())
        server.quit()
        log(f"[邮件] 已发送: {subject} (收件人: {len(receivers)}, 抄送: {len(cc_list)})")
        return True
    except Exception as e:
        log(f"[邮件] 发送失败: {e}")
        return False


def send_batch_notification(updates: list):
    """把同一轮检测到的多个依赖更新合并成一封邮件发送。

    updates: list of dict，每项包含 name / old_version / new_version / url
    """
    if not updates:
        return False
    total = len(updates)
    subject = f"[版本更新] 检测到 {total} 个依赖更新"
    rows = ""
    for u in updates:
        rows += f"""
        <tr>
            <td style="padding: 8px; border-bottom: 1px solid #eee; font-weight: bold;">{u.get('name', '')}</td>
            <td style="padding: 8px; border-bottom: 1px solid #eee;">{u.get('old_version', '') or '首次检测'}</td>
            <td style="padding: 8px; border-bottom: 1px solid #eee; color: #e74c3c; font-weight: bold;">{u.get('new_version', '')}</td>
            <td style="padding: 8px; border-bottom: 1px solid #eee;"><a href="{u.get('url', '')}" style="color: #2193b0; text-decoration: none;">查看来源</a></td>
        </tr>"""
    body = f"""
<html>
<body style="font-family: Arial, sans-serif; color: #333;">
    <h2 style="color: #2193b0;">版本更新通知</h2>
    <p>本次共检测到 <strong>{total}</strong> 个依赖更新：</p>
    <table style="border-collapse: collapse; width: 100%; max-width: 700px;">
        <tr style="background: #f5f7fa;">
            <th style="padding: 8px; border-bottom: 2px solid #2193b0; text-align: left;">监控项目</th>
            <th style="padding: 8px; border-bottom: 2px solid #2193b0; text-align: left;">旧版本</th>
            <th style="padding: 8px; border-bottom: 2px solid #2193b0; text-align: left;">新版本</th>
            <th style="padding: 8px; border-bottom: 2px solid #2193b0; text-align: left;">来源</th>
        </tr>
        {rows}
    </table>
    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由版本监控程序自动发送</p>
</body>
</html>
"""
    return _build_and_send(subject, body)


def send_version_notification(name: str, old_version: str, new_version: str, url: str):
    """发送单个依赖的更新通知（等价于一封只含一项的批量邮件）。"""
    return send_batch_notification(
        [{"name": name, "old_version": old_version, "new_version": new_version, "url": url}]
    )