<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		%%%headsort
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	        <xsl:template match="classified_candidate_index">
			<table style="clear:both;" class="sortable">
				<tr><th>Class</th><th>Obs</th><th>Conf</th><th>Name</th><th>Coord</th><th>Period</th><th>Fold SNR</th></tr>
				<xsl:apply-templates select="classified_candidate"/>
			</table>

		</xsl:template>


	<xsl:template match="classified_candidate">
		<tr class="cl{cand_class_int}">
			<xsl:apply-templates select="raw_candidate_matched[prefered='true']"/>
		</tr>
	</xsl:template>
	<xsl:template match="raw_candidate_matched">

		<td><xsl:value-of select="../cand_class_int" /></td>
		<td><xsl:value-of select="../obs_status" /></td>
		<td><xsl:value-of select="../conf_status" /></td>
		<td><a href="/cand/{id}"><xsl:value-of select="../name"/></a></td>
		<td><xsl:value-of select="../coordinate"/></td>
		<td><xsl:value-of select="bary_period"/></td>
		<td><xsl:value-of select="fold_snr" /></td>
	</xsl:template>



	<xsl:template match="*">
	</xsl:template>


</xsl:stylesheet>
