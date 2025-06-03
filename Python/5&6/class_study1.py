class Person:
        def __init__(self, name):
            self.name = name            #姓名
            self._age = 1               #年龄，前面加下划线表示私有属性
        def set_age(self, new_age):     #设置年龄
            if 0 < new_age <= 100:             #如果年龄大于0，则修改年龄
                self._age = new_age     #如果年龄大于0，则赋值给私有属性_age
        def get_age(self):              #获取年龄
            return self._age            #获取私有属性_age的值
        
person = Person("张三")
person.set_age(20)                     # 设置年龄为20
print(f"年龄为{person.get_age()}岁")
print(f"年龄为{person._age}岁")
print(f"姓名为{person.name}，年龄为{person._age}岁")
