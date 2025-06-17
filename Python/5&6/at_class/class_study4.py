class House(object):
    def live(self):
        print("供人居住")
    def test(self):
        print("House类测试")
class Car(object):
    def drive(self):
        print("供人驾驶")
    def test(self):
        print("Car类测试")
class TouringCar(House, Car):
   pass
# class TouringCar(Car, House):
#     pass

tour_car = TouringCar()
tour_car.live()  # 调用House类的方法
tour_car.drive()  # 调用Car类的方法
tour_car.test() # 调用TouringCar类的方法,按照顺序调用,优先调用House类的方法
