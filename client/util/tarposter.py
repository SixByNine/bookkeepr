#!/usr/bin/python
import sys,httplib,os
from urlparse import urlparse
import socket

rooturi = sys.argv[1]

uri = urlparse(rooturi)

file = open(sys.argv[2])

proxy = os.environ.get("http_proxy")

xmlstring = file.read()

httpurl = uri[1]
httpreq = uri[2]

if proxy != None:
	httpurl = urlparse(proxy)[1]
	httpreq = rooturi
	sys.stderr.write("Connecting via proxy\n")

sys.stderr.write("Connecting to %s\n"%httpurl)
sys.stderr.write("Posting to %s\n"%httpreq)

connection = httplib.HTTPConnection(httpurl)
try:
	connection.putrequest("POST",httpreq)
	connection.putheader("User-Agent","BookKeepr-Client")
	connection.putheader("Content-type","application/x-tar")
	connection.putheader("Content-length","%d" % len(xmlstring))
	connection.endheaders()
	connection.send(xmlstring)
	reply = connection.getresponse()
except socket.error:
	sys.stderr.write("A socket error occured whilst posting the request\n")
	sys.stderr.write("Connection refused or broken whilst comunicating with the server\n")
	sys.exit(2)
#print "Response: ",reply.status
#print "headers: ",reply.getheaders()
res = reply.read()
print res
if(reply.status>=200 and reply.status < 400):
	sys.exit(0)
else:
	sys.exit(-1)


