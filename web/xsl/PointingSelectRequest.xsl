<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="pointing_index">
		<table>
			<xsl:apply-templates select="pointing"/>
		</table>
	</xsl:template>

	<xsl:template match="pointing">
		<tr>
			<td><xsl:value-of select="grid_id" /></td><td><xsl:value-of select="target" /></td>
		</tr>
	</xsl:template>

</xsl:stylesheet>
