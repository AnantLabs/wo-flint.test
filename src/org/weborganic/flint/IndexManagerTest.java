package org.weborganic.flint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Before;
import org.junit.Test;
import org.weborganic.flint.IndexManager.Priority;
import org.weborganic.flint.content.Content;
import org.weborganic.flint.content.ContentFetcher;
import org.weborganic.flint.content.ContentId;
import org.weborganic.flint.content.ContentType;
import org.weborganic.flint.content.DeleteRule;
import org.weborganic.flint.log.Logger;
import org.weborganic.flint.query.CombinedSearchQuery;
import org.weborganic.flint.query.GenericSearchQuery;
import org.weborganic.flint.query.SearchResults;
import org.weborganic.flint.query.SearchTermParameter;


public class IndexManagerTest {

  private final static String XML_MIME_TYPE = "text/xml";
  private final static String XSLT_PATH =   "xslt/xmlToIdx.xsl";
  private final static String XSLT_PATH_2 = "xslt/xmlTo2Idx.xsl";
  private final static Map<String, String> XSLT_PARAMS = Collections.singletonMap("type", "xml");
  private final static Map<String, String> XSLT_PARAMS_2 = Collections.singletonMap("type", "xml-2");
  private final static String CONFIG =   "testconfig";
  private final static String CONFIG_2 = "testconfig2";
  private final static DocumentType DOCUMENT_TYPE = new DocumentType();
    
  private IndexManager manager;
  private Index index;
  private IndexConfig config;
  
  private static String data = "data";  
  private static boolean delete = false;
  
  private static class DocumentType implements ContentType {
  }
  
  private class TestRequester implements Requester {
    private final String id;
    public TestRequester(int i) {
      this.id = i+"";
    }
    public boolean equals(Object other) {
      return other instanceof TestRequester && this.id.equals(((TestRequester)other).getRequesterID());
    }
    public String getRequesterID() {
      return this.id;
    }
  }
  
  private class TestLogger implements Logger {
    public void info(String info) {System.out.println(info);}
    public void warn(String warn) {System.out.println(warn);}
    public void debug(String debug) {System.out.println(debug);}
    public void error(String err, Throwable t) {System.err.println(err);t.printStackTrace();}
    public void indexInfo(Requester r, Index i, String info) {System.out.println(info);}
    public void indexWarn(Requester r, Index i, String warn) {System.out.println(warn);}
    public void indexDebug(Requester r, Index i, String debug) {System.out.println(debug);}
    public void indexError(Requester r, Index i, String err, Throwable t) {System.err.println(err);t.printStackTrace();}
  }
  private class TestContentID implements ContentId {
    private final int id;
    public TestContentID(int i) {
      this.id = i;
    }
    public boolean equals(Object other) {
      return other instanceof TestContentID && this.id == ((TestContentID) other).id;
    }
    @Override
    public ContentType getContentType() {
      return DOCUMENT_TYPE;
    }
    public String getID() {
      return this.id + "";
    }
  }
  
  private final class TestContentIDTwo extends TestContentID {
    public TestContentIDTwo(int i) {
      super(i);
    }
  }
  private class TestContent implements Content {
    private final ContentId id;
    public TestContent(ContentId i) {
      this.id = i;
    }
    
    public String getMimeType() {
      return XML_MIME_TYPE;
    }
    
    public InputStream getSource() {
      if (!(id instanceof TestContentID)) return null;
      TestContentID tcid = (TestContentID) id;
      String xml = "<doc>" +
               "<docid>"+tcid.id+"</docid>" +
               "<title>document "+tcid.id+"</title>" +
               "<sort>"+tcid.id+"</sort>" +
               "<author>author"+tcid.id+"</author>" +
               "<para>This is document "+tcid.id+"</para>" +
               "<para>it contains some text for doc"+tcid.id+" and search"+tcid.id+"</para>" +
               "<data>"+data+tcid.id+"</data>" +
             "</doc>";
      try {
        return new ByteArrayInputStream(xml.getBytes("UTF-8"));
      } catch (UnsupportedEncodingException e) {
        return null;
      }
    }
    
    public boolean isDeleted() {
      return delete;
    }
    
    public String getConfigID() {
      if (id instanceof TestContentIDTwo) return CONFIG_2;
      return CONFIG;
    }
    
    public DeleteRule getDeleteRule() {
      return new DeleteRule("docid", id.getID());
    }
  }
  @Before
  public void setUp() throws Exception {
    // create a memory index
    final Directory ramdir = new RAMDirectory();
    this.index = new Index() {
      public Analyzer getAnalyzer() {return new StandardAnalyzer(Version.LUCENE_30);}
      public Directory getIndexDirectory() {return ramdir;}
      public String getIndexID() {return "TestIndex";}
    };
    this.manager = new IndexManager(new ContentFetcher() {
      public Content getContent(ContentId id) {
        if (id instanceof TestContentID) return new TestContent(id);
        return null;
      }
    }, new TestLogger());
    this.config = new IndexConfig();
    this.config.addTemplates(DOCUMENT_TYPE, XML_MIME_TYPE, CONFIG, XSLT_PATH);
    this.config.addTemplates(DOCUMENT_TYPE, XML_MIME_TYPE, CONFIG_2, XSLT_PATH_2);
  }
  @Test
  public void testindex() throws Exception {
    Requester req = new TestRequester(1);
    // ok start the manager now
    this.manager.start();    
    // and add documents to the index
    this.manager.index(new TestContentID(1), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(2), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(3), index, config, req, Priority.HIGH, XSLT_PARAMS);
    Thread.sleep(600);
    // check nb of docs, should be finished by now?
    List<IndexJob> jobs = this.manager.getStatus(this.index);
    assertEquals(0, jobs.size());
    // perform search
    GenericSearchQuery query = new GenericSearchQuery();
    query.add(new SearchTermParameter("content", "search1"));
    SearchResults results = this.manager.query(index, query);
    assertEquals(1, results.getScoreDoc().length);
    assertEquals("document 1", results.getDocument(results.getScoreDoc()[0].doc).getField("title").stringValue());
    results.terminate();
    // perform search 2
    query = new GenericSearchQuery();
    query.add(new SearchTermParameter("content", "search2"));
    results = this.manager.query(index, query);
    assertEquals(1, results.getScoreDoc().length);
    assertEquals("document 2", results.getDocument(results.getScoreDoc()[0].doc).getField("title").stringValue());
    results.terminate();
    // perform search 3
    query = new GenericSearchQuery();
    query.add(new SearchTermParameter("content", "search3"));
    results = this.manager.query(index, query);
    assertEquals(1, results.getScoreDoc().length);
    assertEquals("document 3", results.getDocument(results.getScoreDoc()[0].doc).getField("title").stringValue());
    results.terminate();
  }
  @Test
  public void testAddMultipleToIndex() throws Exception {
    Requester req = new TestRequester(1);
    this.manager.start();
    // add multiple documents now
    this.manager.index(new TestContentIDTwo(7), index, config, req, Priority.HIGH, XSLT_PARAMS_2);
    Thread.sleep(400);
    // check nb of docs, should be finished by now?
    List<IndexJob> jobs = this.manager.getStatus(req);
    assertEquals(0, jobs.size());
    // perform search 3
    GenericSearchQuery query = new GenericSearchQuery();
    String pname = XSLT_PARAMS_2.keySet().iterator().next();
    query.add(new SearchTermParameter(pname, XSLT_PARAMS_2.get(pname)));
    SearchResults results = this.manager.query(index, query);
    assertEquals(2, results.getScoreDoc().length);
    assertEquals("author7", results.getDocument(results.getScoreDoc()[0].doc).getField("author").stringValue());
    assertEquals("author7 number 2", results.getDocument(results.getScoreDoc()[1].doc).getField("author").stringValue());
    results.terminate();
  }
  @Test
  public void testCheckStatus() throws Exception {
    Requester req = new TestRequester(1);
    // ok start the manager now
    this.manager.start();
    // add multiple documents
    this.manager.index(new TestContentID(1), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(2), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(3), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(4), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(5), index, config, req, Priority.HIGH, XSLT_PARAMS);
    // check nb of docs, before it's finished
    List<IndexJob> jobs = this.manager.getStatus();
    assertTrue(jobs.size() > 1);
    // add multiple documents
    this.manager.index(new TestContentID(5), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(6), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(7), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(8), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(10), index, config, req, Priority.HIGH, XSLT_PARAMS);
    // check nb of docs, before it's finished
    jobs = this.manager.getStatus(this.index);
    assertTrue(jobs.size() > 1);
    // add multiple documents
    this.manager.index(new TestContentID(11), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(12), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(13), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(14), index, config, req, Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(15), index, config, req, Priority.HIGH, XSLT_PARAMS);
    // check nb of docs, before it's finished
    jobs = this.manager.getStatus(req);
    assertTrue(jobs.size() > 1);
  }
  @Test
  public void testCheckErrors() throws Exception {
    Requester req = new TestRequester(1);
    // ok start the manager now
    this.manager.start();    
    // add a document with an error
    data = "<invalid xml &";
    this.manager.index(new TestContentID(1), index, config, req, Priority.HIGH, XSLT_PARAMS);
    Thread.sleep(500);
    data = "data";
    // check nb of docs, should be finished
    List<IndexJob> jobs = this.manager.getStatus();
    assertEquals(0, jobs.size());
    // check errors TODO
//    List<IndexJob> errors = this.manager.getErrorJobs();
//    assertEquals(1, errors.size());
//    assertNotNull(errors.get(0).getErrorMessage());
//    assertTrue(errors.get(0).getErrorMessage().startsWith("Failed to create Index XML from Source content"));
//    jobs = this.manager.getErrorJobs(this.index);
//    assertEquals(1, jobs.size());
//    assertNotNull(errors.get(0).getErrorMessage());
//    assertTrue(errors.get(0).getErrorMessage().startsWith("Failed to create Index XML from Source content"));
//    jobs = this.manager.getErrorJobs(req);
//    assertEquals(1, jobs.size());
//    assertNotNull(errors.get(0).getErrorMessage());
//    assertTrue(errors.get(0).getErrorMessage().startsWith("Failed to create Index XML from Source content"));
  }
  @Test
  public void testConcurrentindex() throws Exception {
    // ok start the manager now
    this.manager.start();    
    // and add documents to the index
    ExecutorService threadPool = Executors.newCachedThreadPool();
    List<Callable<String>> tasks = new ArrayList<Callable<String>>();
    tasks.add(new Callable<String>() {
      public String call() throws Exception {
        manager.index(new TestContentID(2), index, config, new TestRequester(1), Priority.HIGH, XSLT_PARAMS);
        return "finished";
      }
    });
    tasks.add(new Callable<String>() {
      public String call() throws Exception {
        manager.index(new TestContentID(1), index, config, new TestRequester(1), Priority.LOW, XSLT_PARAMS);
        return "finished";
      }
    });
    tasks.add(new Callable<String>() {
      public String call() throws Exception {
        manager.index(new TestContentID(3), index, config, new TestRequester(1), Priority.HIGH, XSLT_PARAMS);
        return "finished";
      }
    });
    List<Future<String>> done = threadPool.invokeAll(tasks);
    while (!done.isEmpty()) {
      Future<String> f = done.remove(0);
      assertEquals("finished", f.get());
    }
    // check nb of docs, should be finished
    Thread.sleep(500);
    List<IndexJob> jobs = this.manager.getStatus(this.index);
    assertEquals(0, jobs.size());
    // perform search
    GenericSearchQuery query = new GenericSearchQuery();
    query.add(new SearchTermParameter("content", "search1"));
    SearchResults results = this.manager.query(index, query);
    assertEquals(1, results.getScoreDoc().length);
    assertEquals("document 1", results.getDocument(results.getScoreDoc()[0].doc).getField("title").stringValue());
    results.terminate();
    // perform search 2
    query = new GenericSearchQuery();
    query.add(new SearchTermParameter("content", "search2"));
    results = this.manager.query(index, query);
    assertEquals(1, results.getScoreDoc().length);
    assertEquals("document 2", results.getDocument(results.getScoreDoc()[0].doc).getField("title").stringValue());
    results.terminate();
    // perform search 3
    query = new GenericSearchQuery();
    query.add(new SearchTermParameter("content", "search3"));
    results = this.manager.query(index, query);
    assertEquals(1, results.getScoreDoc().length);
    assertEquals("document 3", results.getDocument(results.getScoreDoc()[0].doc).getField("title").stringValue());
    results.terminate();
  }
  @Test
  public void testSort() throws Exception {
    // ok start the manager now
    this.manager.start();
    // and add documents to the index
    this.manager.index(new TestContentID(3), index, config, new TestRequester(1), Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(1), index, config, new TestRequester(1), Priority.HIGH, XSLT_PARAMS);
    this.manager.index(new TestContentID(2), index, config, new TestRequester(1), Priority.HIGH, XSLT_PARAMS);
    Thread.sleep(500);
    // check nb of docs, should be finished by now?
    List<IndexJob> jobs = this.manager.getStatus(this.index);
    assertEquals(0, jobs.size());
    // perform search
    GenericSearchQuery query = new GenericSearchQuery();
    query.add(new SearchTermParameter("title", "document"));
    // sort by field named 'sort'
    query.setSort(new Sort(new SortField("sort", SortField.STRING)));
    SearchResults results = this.manager.query(index, query);
    // make sure the order is correct
    assertEquals(3, results.getScoreDoc().length);
    assertEquals("document 1", results.getDocument(results.getScoreDoc()[0].doc).getField("title").stringValue());
    assertEquals("document 2", results.getDocument(results.getScoreDoc()[1].doc).getField("title").stringValue());
    assertEquals("document 3", results.getDocument(results.getScoreDoc()[2].doc).getField("title").stringValue());
    results.terminate();
  }
  
  @Test
  public void testDeleteFromIndex() throws Exception {
    // ok start the manager now
    this.manager.start();    
    // and add document to the index
    this.manager.index(new TestContentID(1), index, config, new TestRequester(1), Priority.HIGH, XSLT_PARAMS);
    Thread.sleep(200);
    // check nb of docs, should be finished by now?
    assertEquals(0, this.manager.getStatus(this.index).size());
    // delete the document now
    delete = true;
    this.manager.index(new TestContentID(1), index, config, new TestRequester(1), Priority.HIGH, XSLT_PARAMS);
    // check nb of docs, should be finished by now?
    Thread.sleep(200);
    delete = false;
    assertEquals(0, this.manager.getStatus(this.index).size());
    // perform search
    GenericSearchQuery query = new GenericSearchQuery();
    query.add(new SearchTermParameter("content", "search1"));
    SearchResults results = this.manager.query(index, query);
    assertEquals(0, results.getScoreDoc().length);
    results.terminate();
  }
  
  @Test
  public void testUpdateIndex() throws Exception {
    // ok start the manager now
    this.manager.start();
    // and add document to the index
    this.manager.index(new TestContentID(1), index, config, new TestRequester(1), Priority.HIGH, XSLT_PARAMS);
    Thread.sleep(400);
    // perform search
    GenericSearchQuery query = new GenericSearchQuery();
    query.add(new SearchTermParameter("data", "data1"));
    SearchResults results = this.manager.query(index, query);
    assertEquals(1, results.getScoreDoc().length);
    assertEquals("document 1", results.getDocument(results.getScoreDoc()[0].doc).getField("title").stringValue());
    results.terminate();
    // update the document now
    String tempdata = data;
    data = "updated";
    this.manager.index(new TestContentID(1), index, config, new TestRequester(1), Priority.HIGH, XSLT_PARAMS);
    // check nb of docs, should be finished by now?
    Thread.sleep(100);
    data = tempdata;
    assertEquals(0, this.manager.getStatus(this.index).size());
    // perform search 2
    query = new GenericSearchQuery();
    query.add(new SearchTermParameter("content", "search1"));
    results = this.manager.query(index, query);
    assertEquals(1, results.getScoreDoc().length);
    assertEquals("updated1", results.getDocument(results.getScoreDoc()[0].doc).getField("data").stringValue());
    results.terminate();
  }
  
  @Test
  public void testCombinedSearch() throws Exception {
    // ok start the manager now
    this.manager.start();
    // and add documents to the index
    for (int i = 1; i < 11; i++)
      this.manager.index(new TestContentID(i), index, config, new TestRequester(1), Priority.HIGH, XSLT_PARAMS);
    Thread.sleep(500);
    // check nb of docs, should be finished by now?
    List<IndexJob> jobs = this.manager.getStatus(this.index);
    assertEquals(0, jobs.size());
    // perform search
    GenericSearchQuery query1 = new GenericSearchQuery();
    query1.add(new SearchTermParameter("content", "search1"));
    GenericSearchQuery query4 = new GenericSearchQuery();
    query4.add(new SearchTermParameter("content", "search4"));
    CombinedSearchQuery combined = new CombinedSearchQuery(query1, query4, Sort.INDEXORDER);
    SearchResults results = this.manager.query(index, combined);
    // make sure the order is correct
    assertEquals(2, results.getScoreDoc().length);
    assertEquals("document 1", results.getDocument(results.getScoreDoc()[0].doc).getField("title").stringValue());
    assertEquals("document 4", results.getDocument(results.getScoreDoc()[1].doc).getField("title").stringValue());
    results.terminate();
    
  }
  
  @Test
  public void testConcurrentSearch() throws Exception {
    // ok start the manager now
    this.manager.start();
    // and add document to the index
    this.manager.index(new TestContentID(1), index, config, new TestRequester(1), Priority.HIGH, XSLT_PARAMS);
    Thread.sleep(400);
    // perform search
    GenericSearchQuery query = new GenericSearchQuery();
    query.add(new SearchTermParameter("data", "data1"));
    SearchResults results1 = this.manager.query(index, query);
    assertEquals(1, results1.getScoreDoc().length);
    assertEquals("document 1", results1.getDocument(results1.getScoreDoc()[0].doc).getField("title").stringValue());
    // update the document now
    String tempdata = data;
    data = "updated";
    this.manager.index(new TestContentID(1), index, config, new TestRequester(1), Priority.HIGH, XSLT_PARAMS);
    // check nb of docs, should be finished by now?
    Thread.sleep(100);
    data = tempdata;
    assertEquals(0, this.manager.getStatus(this.index).size());
    // perform search 2
    query = new GenericSearchQuery();
    query.add(new SearchTermParameter("content", "search1"));
    SearchResults results2 = this.manager.query(index, query);
    assertEquals(1, results2.getScoreDoc().length);
    assertEquals("updated1", results2.getDocument(results2.getScoreDoc()[0].doc).getField("data").stringValue());
    // ok check that old search results are still valid
    assertEquals("data1", results1.getDocument(results1.getScoreDoc()[0].doc).getField("data").stringValue());
    results1.terminate();
    results2.terminate();
  }
  
}
