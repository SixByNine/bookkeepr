<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		<html>
			<head>
				<link href="/web/css/root.css" type="text/css" rel="stylesheet"/>
			</head>
			<body>
				<xsl:apply-templates />
			</body>

		</html>
	</xsl:template>

	<xsl:template match="bookkeepr_host">
		<div class="host_status">
			<!--<img class="host_status" src="/web/img/srv{origin_id}.png" alt="Server: {origin_id}" title="Server: {origin_id}"/><br />-->
                        <div class="hostnumber"><xsl:value-of select="origin_id" /></div>
			<img class="host_status" style="clear:left;" src="/web/img/{status}.png" alt="Status: {status}" title="Status: {status}"/>
		</div>


	</xsl:template>

</xsl:stylesheet>
