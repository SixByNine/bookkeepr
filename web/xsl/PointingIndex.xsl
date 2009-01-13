<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="pointing_index">
		<form action="/obs/xmlify/cand/lists" method="post">
			<table>
				<tr>
					<th></th><th>GridId</th><th>RA</th><th>Dec</th><th>N<sub>obs</sub></th><th><acronym title="To be observed">Obs?</acronym></th>
					<th>T<sub>obs</sub></th><th>Rise</th><th>Set</th>
				</tr>
				<xsl:apply-templates />
			</table>
		</form>
	</xsl:template>

	<xsl:template match="pointing">
		<tr>
			<td><input type="checkbox" name="ids[]" value="{id}"/></td><td><xsl:value-of select="grid_id" /></td><td><xsl:value-of select="target" /></td><td></td><td>?</td><td><xsl:value-of select="to_observe" /></td><td></td><td></td><td></td>
		</tr>
	</xsl:template>

	<xsl:template match="extended_pointing">
		<tr>
			<td><input type="checkbox" name="ids[]" value="{id}"/></td>
			<td><a href="/obs/eptg/{id}"><xsl:value-of select="grid_id" /></a></td><td><xsl:value-of select="ra_str" /></td><td><xsl:value-of select="dec_str" /></td>
			<td><xsl:value-of select="count(psrxml)" />&#160;<sub><a href="/obs/eptg/{id}" onclick="showhide('{id}');return false;">V</a></sub>&#160;</td><td><img src="/web/img/obs{to_be_observed}.png" alt="{to_be_observed}" /></td><td><xsl:value-of select="tobs"/></td>
			<td><xsl:value-of select="rise"/></td><td><xsl:value-of select="set"/></td>
		</tr>
		<tr>
			<td colspan="8">
				<table  id="{id}" style="display:none; width:100%; border-width: 1px 1px 1px 1px;">
					<tr><th></th><th>GridID</th><th>Beam</th><th>MJD</th><th>Position</th><th>T<sub>obs</sub></th></tr>
					<xsl:apply-templates select="psrxml">
						<xsl:sort select="day_of_observation" data-type="number"/>
						<xsl:sort select="midnight_to_first_sample" data-type="number"/>
						<xsl:sort select="receiver_beam" data-type="number"/>
					</xsl:apply-templates>
				</table>

			</td>
		</tr>
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
