<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="pointing">
		<h2>Pointing: <xsl:value-of select="grid_id" /></h2>
		<table style="clear:both;">


		</table>

	</xsl:template>


</xsl:stylesheet>
