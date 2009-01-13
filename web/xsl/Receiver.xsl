<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="receiver">
		<h2>Receiver: <xsl:value-of select="name" /></h2>
		<table style="clear:both;">
			<tr><th>Key</th><th>Value</th></tr>




		</table>

	</xsl:template>


</xsl:stylesheet>
