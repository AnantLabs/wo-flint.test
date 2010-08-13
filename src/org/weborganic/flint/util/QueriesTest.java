package org.weborganic.flint.util;

import static org.junit.Assert.assertEquals;

import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.TermQuery;
import org.junit.Test;

/**
 * 
 * @author Christophe Lauret
 */
public final class QueriesTest {
  
  @Test
  public void testToTermOrPhraseQuery() {
    assertEquals(TermQuery.class, Queries.toTermOrPhraseQuery("test", "test").getClass());
    assertEquals(PhraseQuery.class, Queries.toTermOrPhraseQuery("test", "\"test\"").getClass());
    assertEquals(PhraseQuery.class, Queries.toTermOrPhraseQuery("test", "\"New York\"").getClass());
  }

}
