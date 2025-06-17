import math

class Circle:
    def __init__(self, radius):
        self.radius = radius
    def get_perimeter(self):
        return 2 * math.pi * self.radius
    def get_area(self):
        return math.pi * self.radius ** 2

circle = Circle(radius=float(input("输入半径：")))

area = circle.get_area()
perimeter = circle.get_perimeter()

print(f"圆的半径为：{circle.radius}")
print(f"圆的面积为：{area:.2f}")
print(f"圆的周长为：{perimeter:.2f}")