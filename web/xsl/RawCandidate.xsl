<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="raw_candidate">
		<h3><xsl:value-of select="source_id" /></h3>
		<table style="clear:both;">
			<tr><th>Coordinate</th><td><xsl:value-of select="coordinate" /></td></tr>
			<tr><th>Cand List</th><td><a href="/cand/{candidate_list_id}">Get CandList</a></td></tr>

		</table>
		<p>
		<img style="float:left; clear:both;" src="/cand/{id}/800x600.png" />
	</p>
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="raw_candidate_section">
		<h4><xsl:value-of select="@name" /></h4>
		<table style="clear:both;">
			<tr><th>Best Bary Period</th><td><xsl:value-of select="best_bary_period" /></td></tr>
		</table>
	</xsl:template>

	<xsl:template match="*">
	</xsl:template>

</xsl:stylesheet>
