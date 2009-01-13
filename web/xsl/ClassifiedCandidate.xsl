<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="classified_candidate">

		<h3><xsl:value-of select="name" /> (Class <xsl:value-of select="cand_class_int" />)</h3>
		<p style="clear:both;">
			Candidate Coordinate: <xsl:value-of select="coordinate" />
		</p>
		<p>
			<xsl:for-each select="raw_candidate_matched[prefered='true']">
				<h4>Prefered <a href="/cand/{id}">Raw Candidate</a> Plot</h4>
				<img style="float:left; clear:both;" src="/cand/{id}/800x600.png" />
			</xsl:for-each>
		</p>

		<h4>Associated candidates</h4>
		<table style="clear:both;">
			<tr><th>Source</th><th>Harmonic Type</th><th>Period</th><th>Fold SNR</th></tr>
			<xsl:apply-templates select="raw_candidate_matched"/>
		</table>

	</xsl:template>
	<xsl:template match="raw_candidate_matched">
		<tr>
			<xsl:choose>
				<xsl:when test=".[confirmed='true']">
					<td><acronym title="Association confirmed by a user">User</acronym></td>
				</xsl:when>
				<xsl:otherwise>
					<td><acronym title="Association suggested by the database">Auto</acronym></td>
				</xsl:otherwise>
			</xsl:choose>

			<td><xsl:value-of select="harm_type"/></td>
			<td><a href="/cand/{id}"><xsl:value-of select="bary_period" /></a></td>
			<td><xsl:value-of select="fold_snr"/></td>
		</tr>
	</xsl:template>



	<xsl:template match="*">
	</xsl:template>


</xsl:stylesheet>
