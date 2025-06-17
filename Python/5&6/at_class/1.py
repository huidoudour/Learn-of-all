num_one = float(input("请输入一个数：\n"))
num_two = float(input("请输入一个数：\n"))
try:
    print("结果为",num_one / num_two)
except ZeroDivisionError as error:
    print("出错了，原因:", error)



