package org.weborganic.flint.util;

import static org.junit.Assert.assertEquals;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
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

  @Test
  public void testSubstitute() throws ParseException {
    String[] fields = new String[]{"title", "body"};
    MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_30, fields, new StandardAnalyzer(Version.LUCENE_30));
    Query q = parser.parse("Come to \"New York\" with pen*");
    Query e = parser.parse("Came to \"New Orleans\" with pen*");
    Query s = Queries.substitute(q, new Term("title", "come"), new Term("title", "came"));
    s = Queries.substitute(s, new Term("body", "come"), new Term("body", "came"));
    s = Queries.substitute(s, new Term("title", "york"), new Term("title", "orleans"));
    s = Queries.substitute(s, new Term("body", "york"), new Term("body", "orleans"));
    assertEquals(e.toString(), s.toString());
  }

}
