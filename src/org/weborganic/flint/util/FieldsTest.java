package org.weborganic.flint.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * 
 * @author Christophe Lauret
 * @version 13 August 2010
 */
public class FieldsTest {

  @Test
  public void testAsBoostMap() {
    
  }

  @Test
  public void testFilterNames() {

  }

  @Test
  public void testIsValidName() {
    assertTrue(Fields.isValidName("test"));
    assertFalse(Fields.isValidName(null));
    assertFalse(Fields.isValidName(""));
  }

  @Test
  public void testToValues() {
    String text = null;
    List<String> values = null;
    // Big
    text = "Big";
    values = Collections.singletonList("Big");
    assertEquals(values.size(), Fields.toValues(text).size());
    assertEquals(values, Fields.toValues(text));
    // Big Bang
    text = "Big bang";
    values = Arrays.asList("Big", "bang");
    assertEquals(values.size(), Fields.toValues(text).size());
    assertEquals(values, Fields.toValues(text));    
    // Big Bang
    text = "   Big   bang ";
    values = Arrays.asList("Big", "bang");
    assertEquals(values.size(), Fields.toValues(text).size());
    assertEquals(values, Fields.toValues(text));    
    // The "Big bang"
    text = "The \"Big bang\"";
    values = Arrays.asList("The", "\"Big bang\"");
    assertEquals(values.size(), Fields.toValues(text).size());
    assertEquals(values, Fields.toValues(text));
    // The "Big bang"
    text = "The \"Big bang";
    values = Arrays.asList("The", "\"Big", "bang");
    assertEquals(values.size(), Fields.toValues(text).size());
    assertEquals(values, Fields.toValues(text));
  }
}
