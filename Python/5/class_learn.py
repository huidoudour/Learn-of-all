class Mvexception(Exception):
    pass
r=float(input("请输入半径r："))
try:
    if r<=0:
        raise Mvexception("输入的半径必须大于0")
    else:
        print("这个圆的面积是：",3.14*r*r)
except Mvexception as err:
    print("Error",err)