<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/">
		%%%head
        <xsl:apply-templates />
		%%%tail
    </xsl:template>
    <xsl:template match="log">
        <h2>
            <xsl:value-of select="current_time"/>
        </h2>
        <table class="floating" style="clear:both;">
            <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Message</th>
                <th>Origin</th>
            </tr>
            <xsl:apply-templates select="log_item" />
        </table>
        <ul class="vertmenu">
            <li>
                <a href="/log/clear">Clear Alerts
                </a>
            </li>
            <li>
                <a href="/sys/testlog">Create Test Events
                </a>
            </li>
	    <li>
		    <a href="/sys/hashcheck">View Checksums</a>
	    </li>
            <li>
                <a href="/sys/down">Shut down server
                </a>
            </li>
        </ul>
    </xsl:template>
    <xsl:template match="log_item">
        <tr class="log{type}">
            <td class="logdate">
                <xsl:value-of select="date"/>
            </td>
            <td class="logtype">
                <xsl:value-of select="type"/>
            </td>
            <td class="logmessage">
                <xsl:value-of select="message"/>
            </td>
            <td class="logorigin">
                <xsl:value-of select="origin"/>
            </td>
        </tr>
    </xsl:template>
</xsl:stylesheet>
