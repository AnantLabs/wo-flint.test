package org.weborganic.flint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Before;
import org.junit.Test;


public class IndexIOTest {

  private IndexIO indexIO;
  
  @Before
  public void setUp() throws Exception {
    // create a memory index
    final Directory ramdir = new RAMDirectory();
    Index index = new Index() {
      
      public Analyzer getAnalyzer() {
        return new StandardAnalyzer(Version.LUCENE_30);
      }
      
      public Directory getIndexDirectory() {
        return ramdir;
      }
      
      public String getIndexID() {
        return "TestIndex";
      }
    };
    indexIO = new IndexIO(index);
  }

  @Test
  public void testAddDocuments() throws Exception {
    List<Document> docs = new ArrayList<Document>();
    Document doc1 = new Document();
    doc1.add(new Field("field1", "this is value1 for field1 in document1", Field.Store.YES, Field.Index.ANALYZED));
    doc1.add(new Field("field2", "this is value2 for field2 in document1", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc1.add(new Field("field3", "this is value3 for field3 in document1", Field.Store.NO, Field.Index.ANALYZED));
    doc1.add(new Field("field4", "this is value4 for field4 in document1", Field.Store.NO, Field.Index.NOT_ANALYZED));
    docs.add(doc1);
    Document doc2 = new Document();
    doc2.add(new Field("field1", "this is value1 for field1 in document2", Field.Store.YES, Field.Index.ANALYZED));
    doc2.add(new Field("field2", "this is value2 for field2 in document2", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc2.add(new Field("field3", "this is value3 for field3 in document2", Field.Store.NO, Field.Index.ANALYZED));
    doc2.add(new Field("field4", "this is value4 for field4 in document2", Field.Store.NO, Field.Index.NOT_ANALYZED));
    docs.add(doc2);
    Document doc3 = new Document();
    doc3.add(new Field("field1", "this is value1 for field1 in document3", Field.Store.YES, Field.Index.ANALYZED));
    doc3.add(new Field("field2", "this is value2 for field2 in document3", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc3.add(new Field("field3", "this is value3 for field3 in document3", Field.Store.NO, Field.Index.ANALYZED));
    doc3.add(new Field("field4", "this is value4 for field4 in document3", Field.Store.NO, Field.Index.NOT_ANALYZED));
    docs.add(doc3);
    indexIO.updateDocuments(null, docs);
    // ok now check that they are there
    IndexSearcher newSearcher = indexIO.bookSearcher();
    assertEquals(3, newSearcher.maxDoc());
    indexIO.releaseSearcher(newSearcher);
  }

  @Test
  public void testDifferentSearchers() throws Exception {
    // add one doc
    Document doc1 = new Document();
    doc1.add(new Field("field1", "this is value1 for field1 in document1", Field.Store.YES, Field.Index.ANALYZED));
    doc1.add(new Field("field2", "this is value2 for field2 in document1", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc1.add(new Field("field3", "this is value3 for field3 in document1", Field.Store.NO, Field.Index.ANALYZED));
    doc1.add(new Field("field4", "this is value4 for field4 in document1", Field.Store.NO, Field.Index.NOT_ANALYZED));
    indexIO.updateDocuments(null, Collections.singletonList(doc1));
    IndexSearcher searcher = indexIO.bookSearcher();
    assertEquals(1, searcher.maxDoc());
    indexIO.releaseSearcher(searcher);
    // add more
    List<Document> docs = new ArrayList<Document>();
    Document doc2 = new Document();
    doc2.add(new Field("field1", "this is value1 for field1 in document2", Field.Store.YES, Field.Index.ANALYZED));
    doc2.add(new Field("field2", "this is value2 for field2 in document2", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc2.add(new Field("field3", "this is value3 for field3 in document2", Field.Store.NO, Field.Index.ANALYZED));
    doc2.add(new Field("field4", "this is value4 for field4 in document2", Field.Store.NO, Field.Index.NOT_ANALYZED));
    docs.add(doc2);
    Document doc3 = new Document();
    doc3.add(new Field("field1", "this is value1 for field1 in document3", Field.Store.YES, Field.Index.ANALYZED));
    doc3.add(new Field("field2", "this is value2 for field2 in document3", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc3.add(new Field("field3", "this is value3 for field3 in document3", Field.Store.NO, Field.Index.ANALYZED));
    doc3.add(new Field("field4", "this is value4 for field4 in document3", Field.Store.NO, Field.Index.NOT_ANALYZED));
    docs.add(doc3);
    indexIO.updateDocuments(null, docs);
    // make sure old searcher is still usable and is still in the same state
    assertEquals(1, searcher.maxDoc());
    // ok now check that all docs are there
    IndexSearcher newSearcher = indexIO.bookSearcher();
    assertEquals(3, newSearcher.maxDoc());
    indexIO.releaseSearcher(newSearcher);
    // make sure searchers are different
    assertNotSame(searcher, newSearcher);
    // make sure that getting a new searcher would return the same as
    assertSame(newSearcher, indexIO.bookSearcher());
  }

  @Test
  public void testCommit() throws Exception {
    List<Document> docs = new ArrayList<Document>();
    Document doc1 = new Document();
    doc1.add(new Field("field1", "this is value1 for field1 in document1", Field.Store.YES, Field.Index.ANALYZED));
    doc1.add(new Field("field2", "this is value2 for field2 in document1", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc1.add(new Field("field3", "this is value3 for field3 in document1", Field.Store.NO, Field.Index.ANALYZED));
    doc1.add(new Field("field4", "this is value4 for field4 in document1", Field.Store.NO, Field.Index.NOT_ANALYZED));
    docs.add(doc1);
    Document doc2 = new Document();
    doc2.add(new Field("field1", "this is value1 for field1 in document2", Field.Store.YES, Field.Index.ANALYZED));
    doc2.add(new Field("field2", "this is value2 for field2 in document2", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc2.add(new Field("field3", "this is value3 for field3 in document2", Field.Store.NO, Field.Index.ANALYZED));
    doc2.add(new Field("field4", "this is value4 for field4 in document2", Field.Store.NO, Field.Index.NOT_ANALYZED));
    docs.add(doc2);
    Document doc3 = new Document();
    doc3.add(new Field("field1", "this is value1 for field1 in document3", Field.Store.YES, Field.Index.ANALYZED));
    doc3.add(new Field("field2", "this is value2 for field2 in document3", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc3.add(new Field("field3", "this is value3 for field3 in document3", Field.Store.NO, Field.Index.ANALYZED));
    doc3.add(new Field("field4", "this is value4 for field4 in document3", Field.Store.NO, Field.Index.NOT_ANALYZED));
    docs.add(doc3);
    indexIO.updateDocuments(null, docs);
    indexIO.maybeCommit();
    // ok now check that they are there
    IndexSearcher newSearcher = indexIO.bookSearcher();
    assertEquals(3, newSearcher.maxDoc());
    indexIO.releaseSearcher(newSearcher);
  }

  
  
}
