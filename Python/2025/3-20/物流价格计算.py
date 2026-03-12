print("请选择寄件服务区'华东地区：01'、'华南地区：02'、'华北地区：03'")
cit = input("请输入需要寄件的区域")
weight = float(input("请输入寄件的货物重量(kg)："))
if cit == '01':
    if weight <= 2:
        print("该地区和该寄件品需要的价格为13元")
    elif weight > 2:
        print("超过2kg的价格为：",weight*3+13,"元")
elif cit == '02':
    if weight <= 2:
        print("该地区和该寄件品需要的价格为12元")
    elif weight > 2:
        print("超过2kg的价格为：",weight*2+12,"元")
elif cit == '03':
    if weight <= 2:
        print("该地区和该寄件品需要的价格为14元")
    elif weight > 2:
        print("超过2kg的价格为：",weight*4+14,"元")
else:
    print("你的输入有误")