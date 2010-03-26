<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="xml" indent="yes" encoding="utf-8"/>
  
  <xsl:param name="type" />
  
  <xsl:template match="/doc">
    <documents>
      <document>
        <field store="yes" index="not-analyzed" name="docid"><xsl:value-of select="docid" /></field>
        <field store="yes" index="analyzed" name="type"><xsl:value-of select="$type" /></field>
        <field store="yes" index="analyzed" name="sort"><xsl:value-of select="sort" /></field>
        <field store="yes" index="analyzed" name="title"><xsl:value-of select="title" /></field>
        <field store="yes" index="analyzed" name="author"><xsl:value-of select="author" /></field>
        <field store="yes" index="analyzed" name="data"><xsl:value-of select="data" /></field>
        <field store="yes" index="analyzed-no-norms" name="content">
          <xsl:for-each select="para"><xsl:value-of select="concat(., ' ')" /></xsl:for-each>
        </field>
      </document>
    </documents>
  </xsl:template>
  
</xsl:stylesheet>


