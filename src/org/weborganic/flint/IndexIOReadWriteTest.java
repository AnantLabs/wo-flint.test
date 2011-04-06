package org.weborganic.flint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class IndexIOReadWriteTest {

  private final static String IndexDir = "C:/Work/index-test";
  
  private IndexIOReadWrite ramIndexIO = null;
  
  @Before
  public void setUp() throws Exception {
//     create a memory index
    final Directory ramdir = new RAMDirectory();
    Index index = new Index() {
      
      public Analyzer getAnalyzer() {
        return new StandardAnalyzer(Version.LUCENE_30);
      }
      
      public Directory getIndexDirectory() {
        return ramdir;
      }
      
      public String getIndexID() {
        return "TestRAMIndex";
      }
    };
    ramIndexIO = new IndexIOReadWrite(index);
  }
  
  private IndexIO createNewFileIndex(String dir) throws IOException {
    // file system index
    final File f = new File(dir);
    f.mkdir();
    // remove any existing index
    for (File afile : f.listFiles()) afile.delete();
    // create new index then
    final Directory filedir = FSDirectory.open(f);
    return new IndexIOReadWrite(new Index() {
      public Analyzer getAnalyzer() {return new StandardAnalyzer(Version.LUCENE_30);}
      public Directory getIndexDirectory() {return filedir;}
      public String getIndexID() {return "TestFileSystemIndex "+f.getName();}
    });
  }

  @Test
  public void testIndexes() throws Exception {
    OpenIndexManager.setMaxOpenedIndexes(5);
    int nbOfIndexes = 10;
    int docsInIndex = 10 * 100;
    int fieldsInDoc = 10;
    List<IndexIO> indexes = new ArrayList<IndexIO>();
    for (int a = 1; a <= nbOfIndexes; a++) {
      IndexIO io = createNewFileIndex(IndexDir+"/index"+a);
      indexes.add(io);
      for (int i = 1; i <= docsInIndex; i++) {
        Document doc = new Document();
        for (int j = 0; j < fieldsInDoc; j++)
          doc.add(new Field("field"+j, "this is the value "+j, Field.Store.YES, Field.Index.ANALYZED));
        io.updateDocuments(null, Collections.singletonList(doc));
        if (i % 50 == 0) {
          // ok now check that they are there
          IndexSearcher newSearcher = io.bookSearcher();
          newSearcher.search(new TermQuery(new Term("field3", "this is the value3")), 10);
          assertEquals(i, newSearcher.maxDoc());
          io.releaseSearcher(newSearcher);
        }
      }
    }
    // close half of them
    OpenIndexManager.closeOldReaders();
    // searching now, it should re-create a reader for the ones that were closed
    for (IndexIO io : indexes) {
      // ok now check that they are there
      IndexSearcher newSearcher = io.bookSearcher();
      newSearcher.search(new TermQuery(new Term("field3", "this is the value3")), 10);
      assertEquals(docsInIndex, newSearcher.maxDoc());
      io.releaseSearcher(newSearcher);
    }
  }
  
  @After
  public void finish() throws IndexException {
    this.ramIndexIO.stop();
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
    ramIndexIO.updateDocuments(null, docs);
    // ok now check that they are there
    IndexSearcher newSearcher = ramIndexIO.bookSearcher();
    assertEquals(3, newSearcher.maxDoc());
    ramIndexIO.releaseSearcher(newSearcher);
  }

  @Test
  public void testDifferentSearchers() throws Exception {
    // add one doc
    Document doc1 = new Document();
    doc1.add(new Field("field1", "this is value1 for field1 in document1", Field.Store.YES, Field.Index.ANALYZED));
    doc1.add(new Field("field2", "this is value2 for field2 in document1", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc1.add(new Field("field3", "this is value3 for field3 in document1", Field.Store.NO, Field.Index.ANALYZED));
    doc1.add(new Field("field4", "this is value4 for field4 in document1", Field.Store.NO, Field.Index.NOT_ANALYZED));
    ramIndexIO.updateDocuments(null, Collections.singletonList(doc1));
    IndexSearcher searcher = ramIndexIO.bookSearcher();
    assertEquals(1, searcher.maxDoc());
    ramIndexIO.releaseSearcher(searcher);
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
    ramIndexIO.updateDocuments(null, docs);
    // make sure old searcher is still usable and is still in the same state
    assertEquals(1, searcher.maxDoc());
    // ok now check that all docs are there
    IndexSearcher newSearcher = ramIndexIO.bookSearcher();
    assertEquals(3, newSearcher.maxDoc());
    ramIndexIO.releaseSearcher(newSearcher);
    // make sure searchers are different
    assertNotSame(searcher, newSearcher);
    // make sure that getting a new searcher would return the same as
    assertSame(newSearcher, ramIndexIO.bookSearcher());
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
    ramIndexIO.updateDocuments(null, docs);
    ramIndexIO.maybeCommit();
    // ok now check that they are there
    IndexSearcher newSearcher = ramIndexIO.bookSearcher();
    assertEquals(3, newSearcher.maxDoc());
    ramIndexIO.releaseSearcher(newSearcher);
  }
  
  
  
}
