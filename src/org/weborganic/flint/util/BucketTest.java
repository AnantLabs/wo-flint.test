package org.weborganic.flint.util;

import java.util.Set;

import org.junit.Test;
import org.weborganic.flint.util.Bucket.Entry;

/**
 * A test case for the {@link Bucket} class.
 * 
 * @author Christophe Lauret
 * @version 28 July 2010
 */
public class BucketTest {

  /**
   * 
   */
  @Test public void testOverflow() {
    Bucket<String> bucket = new Bucket<String>(5);
    bucket.add("a", 1);
    bucket.add("b", 11);
    bucket.add("c", 5);
    bucket.add("d", 5);
    bucket.add("e", 2);
    bucket.add("f", 0);
    bucket.add("g", 99);
    bucket.add("h", 3);
    Set<Entry<String>> t = bucket.entrySet();
    System.err.println(t);
  }

  /**
   * 
   */
  @Test public void testNormal() {
    Bucket<String> bucket = new Bucket<String>(10);
    bucket.add("a", 1);
    bucket.add("b", 5);
    bucket.add("c", 5);
    bucket.add("d", 0);
    bucket.add("e", 0);
    Set<Entry<String>> t = bucket.entrySet();
    System.err.println(t);
  }
}
