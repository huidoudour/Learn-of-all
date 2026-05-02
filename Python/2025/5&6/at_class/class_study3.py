class Cat(object):
    def __init__(self, color, name):
        self.color = color
    def walk(self):
        print("走猫步~")
class ScottishFold(Cat):
    pass
passfold = ScottishFold("灰色", "折耳")
print(f"{passfold.color}的折耳猫")
passfold.walk()