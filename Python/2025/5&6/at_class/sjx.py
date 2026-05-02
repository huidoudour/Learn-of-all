# import os
import time
import turtle
# turtle.setup(1000,1000,0,0)
turtle.setup(500,500,0,0)
size = 20
turtle.pensize(size)
turtle.color("red")
length = 200
turtle.seth(0)
turtle.fd(length)
turtle.seth(120)
turtle.fd(length)
turtle.seth(240)
turtle.fd(length)

# os.system("pause")

time.sleep(1)
turtle.bye()
