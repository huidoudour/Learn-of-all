class Person(object):
    def _hello(self):
        print("打招呼")
    
class qwq(Person):
    def say_hello(self):
        print("吃了嘛？")

qwq = qwq()
qwq.say_hello()  # 调用qwq类的方法