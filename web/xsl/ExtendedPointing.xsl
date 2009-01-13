<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="extended_pointing">
		<h2>Pointing: <xsl:value-of select="grid_id" /></h2>
		<table style="clear:both;">
			<tr><th>Key</th><th>Value</th></tr>
			<tr><td>GridID</td><td><xsl:value-of select="grid_id" /></td></tr>
			<tr><td>Centre RA</td><td><xsl:value-of select="ra_str" /></td></tr>
			<tr><td>Centre Dec</td><td><xsl:value-of select="dec_str" /></td></tr>
			<tr><td>Centre Gl</td><td><xsl:value-of select="gl_str" /></td></tr>
			<tr><td>Centre Gb</td><td><xsl:value-of select="gb_str" /></td></tr>
			<tr><td>To Observe</td><td><xsl:value-of select="to_be_observed" /></td></tr>
			<tr><td>Rise LST</td><td><xsl:value-of select="rise" /></td></tr>
			<tr><td>Set LST</td><td><xsl:value-of select="set" /></td></tr>
			<!--	<xsl:for-each select="expected_coverage/coordinate">
				<tr><td>Estimated Beam Coord</td><td><xsl:value-of select="." /></td></tr>
			</xsl:for-each>-->
		</table>
		<h4>Observations (<xsl:value-of select="count(psrxml)"/>)</h4>
		<table style="clear:both;">
			<tr><th></th><th>GridID</th><th>Beam</th><th>MJD</th><th>Position</th><th>T<sub>obs</sub></th></tr>
			<xsl:apply-templates select="psrxml">
				<xsl:sort select="day_of_observation" data-type="number"/>
				<xsl:sort select="midnight_to_first_sample" data-type="number"/>
				<xsl:sort select="receiver_beam" data-type="number"/>
			</xsl:apply-templates>
		</table>
	</xsl:template>

	<xsl:template match="psrxml">
		<tr>
			<td></td>
			<td><a href="/id/{id}"><xsl:value-of select="source_name" /></a></td>
			<td><xsl:value-of select="receiver_beam"/></td>
			<td><xsl:value-of select="day_of_observation"/></td>
			<td><xsl:value-of select="start_coordinate/coordinate/friendly_eq" /></td>
			<td><xsl:value-of select="actual_obs_time" /></td>

		</tr>

	</xsl:template>




</xsl:stylesheet>
