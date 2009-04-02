#!/usr/bin/python
import sys,httplib,os
from urlparse import urlparse

rooturi = sys.argv[1]

uri = urlparse(rooturi)


proxy = os.environ.get("http_proxy")

httpurl = uri[1]
httpreq = uri[2]
if proxy != None:
	httpurl = urlparse(proxy)[1]
	httpreq = rooturi
	sys.stderr.write("Connecting via proxy\n")

sys.stderr.write("Connecting to %s\n"%httpurl)
sys.stderr.write("DELETE from %s\n"%httpreq)
connection = httplib.HTTPConnection(httpurl)
try:
	connection.putrequest("DELETE",httpreq)
	connection.putheader("User-Agent","BookKeepr-Client")
	connection.endheaders()
	reply = connection.getresponse()

except socket.error:
        print "A socket error occured whilst posting the request"
        print "Connection refused or broken whilst comunicating with the server"
        sys.exit(2)

#sys.stderr.write("Response: %d\n"%reply.status)
#sys.stderr.write("headers: %s\n\n"%reply.getheaders())
res = reply.read()
sys.stdout.write(res)
if(reply.status>=200 and reply.status < 400):
	sys.exit(0)
else:
	sys.exit(-1)


