#!/usr/bin/python

#
#  This python script illustrates fetching information from a CGI program
#  that typically gets its data via an HTML form using a POST method.
#
#  Copyright (c) 2018, Carnegie Mellon University.  All Rights Reserved.
#

import requests
import re

import csv

#  ===> FILL IN YOUR PARAMETERS <===
name = 'E5_10_0.530'
name1 = 'map65'

userId = 'jiangtiq@andrew.cmu.edu'
password = 'jvNgsKOv'
#fileIn = '/Users/kaka/Documents/CMU/searchengine/hw2/QryEval/' + name + '.teIn'
fileIn = '/Users/kaka/Documents/CMU/searchengine/hw5/QryEval/HW5-Train-1.teIn'
#  Form parameters - these must match form parameters in the web page

url = 'http://boston.lti.cs.cmu.edu/classes/11-642/HW/HTS/tes.cgi'
values = { 'hwid' : 'HW1',				# cgi parameter
	   'qrel' : 'topics.701-850.qrel',		# cgi parameter
           'logtype' : 'Detailed',			# cgi parameter
	   'leaderboard' : 'No'				# cgi parameter
           }

#  Make the request

files = {'infile' : (fileIn, open(fileIn, 'rb')) }	# cgi parameter
result = requests.post (url, data=values, files=files, auth=(userId, password))

#  Replace the <br /> with \n for clarity

response = result.content.replace ('<br />', '\n')


index1 = [m.start() for m in re.finditer('map', response)]
index2 = [m.start() for m in re.finditer('R-prec', response)]



#print("index1")
#print(index1)
print("=============================================")

result = {}

m = 0
while m < len(index1):
	print(response[index1[m]:index2[m]])
	m = m + 1


i = 0
for x in index1:
	index1[i] = index1[i] + 20
	i  = i  + 1

with open('result.csv', 'wb') as csvfile:
	fieldnames = [name1]
	writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
	writer.writeheader()
	m = 0
	while m < 19:
		tmp = response[index1[m]:index1[m] + 6]
		writer.writerow({name1: tmp})
		m = m + 1

values_summary = { 'hwid' : 'HW1',				# cgi parameter
	   'qrel' : 'topics.701-850.qrel',		# cgi parameter
           'logtype' : 'Summary',			# cgi parameter
	   'leaderboard' : 'No'				# cgi parameter
           }


files_summary = {'infile' : (fileIn, open(fileIn, 'rb')) }	# cgi parameter
result_summary = requests.post (url, data=values_summary, files=files_summary, auth=(userId, password))

#  Replace the <br /> with \n for clarity

summary = result_summary.content.replace ('<br />', '\n')

pre = [m.start() for m in re.finditer('num_q', summary)]
end = [m.start() for m in re.finditer('</pre>', summary)]


print(summary[pre[0]:end[0]])
#print(summary)




