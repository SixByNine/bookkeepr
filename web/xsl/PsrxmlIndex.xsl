<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="psrxml_index">
			<table>
				<tr>
					<th>GridId</th>
				</tr>
				<xsl:apply-templates />
			</table>
	</xsl:template>

	<xsl:template match="psrxml">
		<tr>
			<td><a href="/id/{id}"><xsl:value-of select="source_name" /></a></td>
		</tr>
	</xsl:template>




</xsl:stylesheet>
