<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		%%%head
		<xsl:apply-templates />
		%%%tail
	</xsl:template>

	<xsl:template match="candidate_list_index">
			<table>
				<tr>
					<th>Name</th>
				</tr>
				<xsl:apply-templates />
			</table>
	</xsl:template>

	<xsl:template match="candidate_list_stub">
		<tr>
			<td><a href="/cand/{id}"><xsl:value-of select="name" /></a></td>
		</tr>
	</xsl:template>

</xsl:stylesheet>
