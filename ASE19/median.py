import os, sys
import numpy


number_list = []
#with open("numOfRelated.txt") as f:
#with open("avgLocOfRelated.txt") as f:
with open("numOfSimilarGH.txt") as f:
        for line in f:
                num = float(line.strip())
                number_list.append(num)

print max(number_list)
print min(number_list)
print numpy.median(number_list)
print numpy.mean(number_list)


count1 = 0
count2 = 0
count3 = 0
count4 = 0
count5 = 0
count6 = 0
for num in number_list:
	if num>0 and num<=5:
		count1 += 1
	if num>5 and num<=10:
		count2 += 1
	if num>10 and num<=15:
		count3 += 1
	if num>15 and num<=20:
		count4 += 1
	if num>20 and num<=25:
		count5 += 1
	if num>30 and num<=35:
		count6 += 1

print count1
print count2
print count3
print count4
print count5
print count6