print("=========================================")
for x in range (1,5):
    print("qwq") 

print("\n")
print("=========================================")
import random
import string

def generate_password(length):
    all_characters = string.ascii_letters + string.digits + string.punctuation
    password = ''.join(random.choice(all_characters) for _ in range(length))
    return password

length = int(input("请输入你想要的密码长度: "))
password = generate_password(length)
print("生成的随机密码是: {password}")

print("=========================================")
import random
# 生成一个1到100之间的随机数
secret_number = random.randint(1, 100)
guess = None
attempts = 0

print("欢迎来到猜数字游戏！")
print("我已经想好了一个1到100之间的数字，快来猜猜看吧！")

while guess != secret_number:
    try:
        guess = int(input("请输入你的猜测: "))
        attempts += 1
        if guess < secret_number:
            print("太小了，请再试一次。")
        elif guess > secret_number:
            print("太大了，请再试一次。")
        else:
            print("恭喜你，猜对了！你总共用了 {attempts} 次尝试。")
    except ValueError:
        print("请输入一个有效的整数。")

print("=========================================")
