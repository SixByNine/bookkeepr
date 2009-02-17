<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="archived_storage">
		<h2><xsl:value-of select="media_type" />: <xsl:value-of select="media_label" /></h2>
		<table style="clear:both;">
			<tr><td>Media Label</td><td><xsl:value-of select="media_label" /></td></tr>
                        <tr><td>Media Type</td><td><xsl:value-of select="media_type" /></td></tr>
                        <tr><td>Current Location</td><td><xsl:value-of select="storage_location" /></td></tr>
		</table>
		                <p style="clear:both;"><a href="/storage/{id}">List Contents</a></p>

		<form  method="POST" action="/web/xmlify/storage/relocate/{media_label}">
			<fieldset>
				<h4 style="clear:both;">Change Location</h4>
				<p  style="clear:both;">
					<input type="hidden" name="class" value="ArchivedStorage" />
					<label for="StorageLocation">New Location</label><input type="text" name="StorageLocation" value="{storage_location}"/>
					<input class="submit" type="submit" value="Change" />
				</p>
			</fieldset>
		</form>
	</xsl:template>


</xsl:stylesheet>
