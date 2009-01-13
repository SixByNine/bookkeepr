#!/usr/bin/python
import sys,httplib,os
from urlparse import urlparse
import socket


def postXml(uri,xmlstring):
	proxy = os.environ.get("http_proxy")
	uri=urlparse(uri)
	
	
	httpurl = uri[1]
	httpreq = uri[2]
	if proxy != None:
		httpurl = urlparse(proxy)[1]
		httpreq = rooturi
	#	sys.stderr.write("Connecting via proxy\n")

	#sys.stderr.write("Connecting to %s\n"%httpurl)
	#sys.stderr.write("Posting to %s\n"%httpreq)
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
	return reply
	

if __name__ == "__main__":
	rooturi = sys.argv[1]


	if len(sys.argv) > 2:
		file = open(sys.argv[2])
	else:
        	file = sys.stdin


	xmlstring = file.read()
	reply = postXml(rooturi,xmlstring)
	res = reply.read()
        print res
        if(reply.status>=200 and reply.status < 400):
                sys.exit(0)
        else:
                sys.exit(-1)

