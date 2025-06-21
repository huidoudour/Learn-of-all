class Myexception(Exception):
    pass
num_1 = float(input("第一条边长："))
num_2 = float(input("第二条边长："))
num_3 = float(input("第三条边长："))
c = max(num_1, num_2, num_3)
a = min(num_1, num_2, num_3)
b = num_1 + num_2 + num_3 - a - c

try:
    if a > 0 and b > 0 and c > 0:
        if a**2 + b**2 == c**2:
            print("这是直角三角形")
            print("这个直角三角形面积为：", 0.5 * a * b)
            print("这个直角三角形周长为：", a + b + c)
        else:
            raise Myexception("这是不是直角三角形")
    else:
        raise Myexception("输入的边长有误")
except Myexception as error:
    print("原因:",error)