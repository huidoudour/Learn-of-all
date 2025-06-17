print("三角形边长判断")
num_1 = float(input("第一条边长："))
num_2 = float(input("第二条边长："))
num_3 = float(input("第三条边长："))
c = max(num_1, num_2, num_3)
a = min(num_1, num_2, num_3)
b = num_1 + num_2 + num_3 - a - c
try:
    if a + b <= c:
        print("这不是一个三角形")
    else:
        try:
            if a**2 + b**2 == c**2:
                print("这是一个直角三角形")
            elif a==b==c:
                print("这是一个三角形")
        except ZeroDivisionError as error:
            print("这不是一个直角三角形",error)
except ZeroDivisionError as error:
    print("输入的边长有误",error)

