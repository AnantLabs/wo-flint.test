package org.weborganic.flint.util;

import junit.framework.TestCase;

/**
 * A test case for the Flint entity resolver.
 * 
 * @author Christophe Lauret
 * @version 29 October 2009
 */
public class FlintEntityResolverTest extends TestCase {

  /**
   * Tests the {FlintEntityResolver#toFileName} method.
   */
  public void testToFileName() {
    // No public ID
    assertNull(FlintEntityResolver.toFileName(null));
    // Public ID does not match prefix
    assertNull(FlintEntityResolver.toFileName("X"));
    // Public ID matched prefix (empty)
    assertNull(FlintEntityResolver.toFileName("-//Weborganic//DTD::Flint "));
    // Public ID matched prefix (correct rules)
    assertEquals("abc.7.dtd", FlintEntityResolver.toFileName("-//Weborganic//DTD::Flint ABC.7"));
    assertEquals("a-bc-.-7.dtd", FlintEntityResolver.toFileName("-//Weborganic//DTD::Flint A BC . 7"));
    // Public ID matched prefix (known DTDs)
    assertEquals("index-document-1.0.dtd", FlintEntityResolver.toFileName("-//Weborganic//DTD::Flint Index Document 1.0"));
    assertEquals("index-document-1.0.dtd", FlintEntityResolver.toFileName("-//Weborganic//DTD::Flint Index Document 1.0//EN"));
  }
}
