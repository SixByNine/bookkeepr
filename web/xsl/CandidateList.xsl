<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="candidate_list">
		<h3><xsl:value-of select="name" />: <xsl:value-of select="completed_date" /></h3>
		<p><a href="/id/{psrxml_id}">Psrxml Header</a></p>
		<table style="clear:both;">
			<tr>
				<th>Bary Period</th><th>Fold Snr</th>
			</tr>
			<xsl:apply-templates />
		</table>
	</xsl:template>

	<xsl:template match="raw_candidate_basic">
		<tr>
			<td><a href="/cand/{id}"><xsl:value-of select="bary_period" /></a></td>
			<td><xsl:value-of select="fold_snr" /></td>

		</tr>
	</xsl:template>

	<xsl:template match="*">
	</xsl:template>

</xsl:stylesheet>
