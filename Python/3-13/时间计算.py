print("中文，ChineseGBK")
print("***********************************")
start_hour = int(input("请输入起始时间的小时数："))
start_minute = int(input("请输入起始时间的分钟数："))

end_hour = int(input("请输入结束时间的小时数："))
end_minute = int(input("请输入结束时间的分钟数："))

start_total_minutes = start_hour * 60 + start_minute

end_total_minutes = end_hour * 60 + end_minute

duration_minutes = end_total_minutes - start_total_minutes
duration_hour = duration_minutes // 60
duration_minute = duration_minutes % 60

print("时间间隔为：",duration_hour,"小时",duration_minute,"分钟")




