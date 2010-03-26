package org.weborganic.flint.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.topologi.diffx.util.CommandLine;

/**
 * A test case for the command line utility class.
 * 
 * @author Christophe Lauret
 * @version 1 March 2010
 */
public class CommandLineTest {

  /**
   * Tests the {@link CommandLine#getParameter(String, String[])} method.
   */
  @Test
  public void testGetParameter() {
    assertNull(CommandLine.getParameter("x", null));
    assertNull(CommandLine.getParameter("x", new String[]{}));
    String[] args = new String[]{"-a", "b", "x", "123"};
    assertNull(CommandLine.getParameter("z", args));
    assertNull(CommandLine.getParameter("123", args));
    assertEquals("b", CommandLine.getParameter("-a", args));
    assertEquals("x", CommandLine.getParameter("b", args));
    assertEquals("123", CommandLine.getParameter("x", args));
  }

  /**
   * Tests the {@link CommandLine#hasSwitch(String, String[])} method.
   */
  @Test
  public void testHasSwitch() {
    assertFalse(CommandLine.hasSwitch("x", null));
    assertFalse(CommandLine.hasSwitch("x", new String[]{}));
    String[] args = new String[]{"-a", "b", "x", "123"};
    assertFalse(CommandLine.hasSwitch("z", args));
    assertTrue(CommandLine.hasSwitch("123", args));
    assertTrue("b", CommandLine.hasSwitch("-a", args));
    assertTrue("x", CommandLine.hasSwitch("b", args));
    assertTrue("123", CommandLine.hasSwitch("x", args));
  }

}
