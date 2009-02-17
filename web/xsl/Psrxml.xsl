<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="psrxml">
		<h2>Observation: <xsl:value-of select="source_name" /></h2>
		<h3>PsrXML header</h3>
		<p style="clear:both;"><a href="/id/raw/{id}">Download psrxml file</a></p>
		<p style="clear:both;"><a href="/storage/{id}">Locate copies on Tape</a></p>
		<table style="clear:both;">
			<tr><th>Key</th><th>Value</th></tr>
			<tr><td>Source Name</td><td><xsl:value-of select="source_name" /></td></tr>
			<tr><td>Source Name (centre beam)</td><td><xsl:value-of select="source_name_centre_beam" /> (<a href="/obs/eptg/{pointing_id}">pointing</a>)</td></tr>
			<tr><td>Receiver Beam</td><td><xsl:value-of select="receiver_beam" /></td></tr>
			<tr><td>Position (Eq)</td><td><xsl:value-of select="start_coordinate/coordinate/friendly_eq" /></td></tr>
			<tr><td>Position (Gal)</td><td><xsl:value-of select="start_coordinate/coordinate/friendly_gal" /></td></tr>
			<tr><td>UTC Date</td><td><xsl:value-of select="substring(utc,0,11)" /></td></tr>
			<tr><td>UTC Time</td><td><xsl:value-of select="substring(utc,12,8)" /></td></tr>
			<tr><td>Local Time</td><td><xsl:value-of select="local_time" /></td></tr>
			<tr><td>Start LST</td><td><xsl:value-of select="lst" /></td></tr>						
			<tr><td>MJD</td><td><xsl:value-of select="day_of_observation" /> +<xsl:value-of select="midnight_to_first_sample" /> <xsl:value-of select="midnight_to_first_sample/@units" /></td></tr>
			<tr><td>Observation Length (<xsl:value-of select="actual_obs_time/@units" />)</td><td><xsl:value-of select="actual_obs_time" /></td></tr>
			<tr><td>Centre Freq Channel 1 (<xsl:value-of select="centre_freq_first_channel/@units" />)</td><td><xsl:value-of select="centre_freq_first_channel" /></td></tr>
			<tr><td>Channel Offset (<xsl:value-of select="channel_offset/@units" />)</td><td><xsl:value-of select="channel_offset" /></td></tr>
			<tr><td>Number of Channels</td><td><xsl:value-of select="number_of_channels" /></td></tr>
			<tr><td>Number of Samples</td><td><xsl:value-of select="number_of_samples" /></td></tr>
			<tr><td>Telescope Name</td><td><a href="/id/{telescope/id}"><xsl:value-of select="telescope/name" /></a></td></tr>
			<tr><td>Receiver Name</td><td><a href="/id/{receiver/id}"><xsl:value-of select="receiver/name" /></a></td></tr>
			<tr><td>Backend Name</td><td><a href="/id/{backend/id}"><xsl:value-of select="backend/name" /></a></td></tr>
			<tr><td>Candidate Lists</td><td><a href="/cand/lists/from/{id}">Get Candidate Lists</a></td></tr>
		</table>

	</xsl:template>


</xsl:stylesheet>
