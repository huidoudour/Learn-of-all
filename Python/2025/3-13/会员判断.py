print("中文，ChineseGBK")
print("***********************************")
user_points = 7500
user_purchases = 800
if user_purchases >= 100 and user_points >= 10000:
    print("钻石会员")
elif user_purchases >= 500 and user_purchases < 1000 and user_points >= 5000 and user_points < 100000:
    print("白金会员")
elif user_purchases >=200 and user_purchases < 500 and user_points >= 2000 and user_points < 5000:
    print("黄金会员")
elif user_purchases >= 100 and user_purchases < 200 and user_points >= 1000 and user_points < 2000:
    print("白银会员")
elif user_purchases >= 50 and user_purchases < 100 and user_points >= 500 and user_points < 1000:
    print("青铜会员")
else:
    print("普通会员")





