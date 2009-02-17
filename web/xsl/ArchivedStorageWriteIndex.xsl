<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="archived_storage_write_index">
		<table>
			<tr>
				<th>Label</th><th>File ID</th><th>Grid ID</th><th>UTC</th><th>Beam</th><th>T<sub>obs</sub></th><th>Size (GB)</th>
			</tr>
			<xsl:apply-templates/>
		</table>
	</xsl:template>
	<xsl:template match="archived_storage_write_extended">
		<tr>
			<td><a href="/id/{archived_storage/id}"><xsl:value-of select="archived_storage/media_label" /></a></td>
			<td><xsl:value-of select="file_label" /></td>
			<td><a href="/id/{psrxml/id}"><xsl:value-of select="psrxml/source_name" /></a></td>
			<td><xsl:value-of select="psrxml/utc" /></td>
			<td><xsl:value-of select="psrxml/receiver_beam" /></td>
			<td><xsl:value-of select="psrxml/actual_obs_time" /></td>
			<td><xsl:value-of select="write_size" /></td>
		</tr>
		
	</xsl:template>


</xsl:stylesheet>
