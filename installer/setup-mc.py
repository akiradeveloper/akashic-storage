from xml.etree.ElementTree import *
import httplib
import os

address = "localhost:10946"
conn = httplib.HTTPConnection("localhost:10946")
conn.request("POST", "/admin/user")
response = conn.getresponse()
data = response.read()
print(data)
elem = fromstring(data)
accessKey = elem.find("AccessKey").text
secretKey = elem.find("SecretKey").text
print("AccessKey: %s" % accessKey)
print("SecretKey: %s" % secretKey)

os.system("mc config alias add akashic-storage http://%s" % address)
os.system("mc config host add http://%s %s %s S3v2" % (address, accessKey, secretKey))
