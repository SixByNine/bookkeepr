<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="telescope">
		<h2>Telescope: <xsl:value-of select="name" /></h2>
		<table style="clear:both;">
			<tr><th>Key</th><th>Value</th></tr>
			<tr><td>Longitude</td><td><xsl:value-of select="longitude" /></td></tr>
			<tr><td>Lattitude</td><td><xsl:value-of select="lattitude" /></td></tr>




		</table>

	</xsl:template>


</xsl:stylesheet>
