<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="archived_storage">
		<h2>ArchivedStorage: <xsl:value-of select="id" /></h2>
		<table style="clear:both;">


		</table>

	</xsl:template>


</xsl:stylesheet>
