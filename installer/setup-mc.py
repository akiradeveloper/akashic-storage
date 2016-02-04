from xml.etree.ElementTree import *
import argparse
import httplib
import os

parser = argparse.ArgumentParser(description="setup mc")
parser.add_argument("--host", default="localhost", help="ip, hostname")
parser.add_argument("--port", default="10946", help="port number")
args = parser.parse_args()

dest = "%s:%s" % (args.host, args.port)
conn = httplib.HTTPConnection(dest)
conn.request("POST", "/admin/user")
response = conn.getresponse()
data = response.read()
print(data)
elem = fromstring(data)
accessKey = elem.find("AccessKey").text
secretKey = elem.find("SecretKey").text
print("AccessKey: %s" % accessKey)
print("SecretKey: %s" % secretKey)

os.system("mc config alias add akashic-storage http://%s" % dest)
os.system("mc config host add http://%s %s %s S3v2" % (dest, accessKey, secretKey))
