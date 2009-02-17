<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="archived_storage_index">
		<table style="clear:both;">
			<tr>
				<th>Media Label</th><th>Media Type</th><th>Location</th>
			</tr>
			<xsl:apply-templates select="archived_storage"/>
		</table>
	</xsl:template>
	<xsl:template match="archived_storage">
			<tr>
				<td><a href="/id/{id}"><xsl:value-of select="media_label" /></a></td>
	                        <td><xsl:value-of select="media_type" /></td>
				<td><xsl:value-of select="storage_location" /></td>
			</tr>
		
	</xsl:template>


</xsl:stylesheet>
