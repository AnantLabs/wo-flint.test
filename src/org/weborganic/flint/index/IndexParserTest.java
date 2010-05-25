package org.weborganic.flint.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.junit.Before;
import org.junit.Test;
import org.weborganic.flint.IndexException;
import org.weborganic.flint.test.TestUtils;
import org.xml.sax.InputSource;

/**
 * Tests the Lucene document parsing.
 */
public class IndexParserTest {

  /**
   * The directory containing the data for this test class.
   */
  private final static File DATA = TestUtils.getDataDirectory(IndexParserTest.class);

  private IndexParser parser = null;

  @Before
  public void setup() throws IndexException {
    this.parser = IndexParserFactory.getInstance();
  }

  /**
   * <field store="yes"      index="no"           name="yn">stored unindexed data</field>
   * <field store="yes"      index="no-norms"     name="yo">stored nonorms data</field>
   * <field store="yes"      index="tokenised"    name="yt">stored tokenised data</field>
   * <field store="yes"      index="un-tokenised" name="yu">stored untokenised data</field>
   * <field store="no"       index="no-norms"     name="no">unstored nonorms data</field>
   * <field store="no"       index="tokenised"    name="nt">unstored tokenised data</field>
   * <field store="no"       index="un-tokenised" name="nu">unstored untokenised data</field>
   * <field store="compress" index="no"           name="cn">compressed unindexed data</field>
   * <field store="compress" index="no-norms"     name="co">compressed nonorms data</field>
   * <field store="compress" index="tokenised"    name="ct">compressed tokenised data</field>
   * <field store="compress" index="un-tokenised" name="cu">compressed untokenised data</field>
   * 
   * @throws FileNotFoundException
   * @throws IndexException
   */
  @Test
  public void testProcess_OK_1_0() throws FileNotFoundException, IndexException {
    List<Document> docs = this.parser.process(new File(DATA, "ok-1.0.xml"));
    assertEquals(2, docs.size());
    // Check document order 
    assertEquals(15, docs.get(0).getFields().size());
    assertNotNull(docs.get(0).getField("author"));
    assertEquals("author1", docs.get(0).getField("author").stringValue());
    assertEquals(1, docs.get(1).getFields().size());
    assertNotNull(docs.get(1).getField("author"));
    assertEquals("author2", docs.get(1).getField("author").stringValue());

    // Check various field types
    Document doc = docs.get(0);
    // Check field not null and flags: Stored, Indexed, Tokenized, OmitNorms 
    assertFieldIsSITO(doc.getField("yn"), true,  false, false, true);
    assertFieldIsSITO(doc.getField("yo"), true,  true,  false, true);
    assertFieldIsSITO(doc.getField("yt"), true,  true,  true,  false);
    assertFieldIsSITO(doc.getField("yu"), true,  true,  false, false);
    assertFieldIsSITO(doc.getField("no"), false, true,  false, true);
    assertFieldIsSITO(doc.getField("nt"), false, true,  true,  false);
    assertFieldIsSITO(doc.getField("nu"), false, true,  false, false);
  }

  @Test
  public void testProcess_OK_2_0() throws FileNotFoundException, IndexException {
    List<Document> docs = this.parser.process(new File(DATA, "ok-2.0.xml"));
    assertEquals(2, docs.size());
    assertEquals("author1", docs.get(0).getField("author").stringValue());
    assertEquals("author2", docs.get(1).getField("author").stringValue());
    Field type = docs.get(0).getField("type");
    assertEquals(true, type.isIndexed());
    assertEquals(true, type.isStored());
    assertEquals(true, type.isTokenized());
    assertEquals(false, type.getOmitNorms());
    assertEquals(false, docs.get(0).getField("data1").isStored());
    assertEquals(false, docs.get(0).getField("data2").isTokenized());
    assertEquals(false, docs.get(0).getField("data3").isTokenized());
    assertEquals(true, docs.get(0).getField("data3").getOmitNorms());
    assertEquals(false, docs.get(0).getField("data4").isIndexed());
    assertEquals(true, docs.get(0).getField("data5").isTokenized());
    assertEquals(true, docs.get(0).getField("data5").getOmitNorms());
  }

  @Test
  public void testProcess_OK_Compatibility() throws FileNotFoundException, IndexException {
    List<Document> docs = this.parser.process(new File(DATA, "ok-compatibility.xml"));
    assertEquals(2, docs.size());
    assertEquals("author1", docs.get(0).getField("author").stringValue());
    assertEquals("author2", docs.get(1).getField("author").stringValue());
    Field type = docs.get(0).getField("type");
    assertEquals(true, type.isIndexed());
    assertEquals(true, type.isStored());
    assertEquals(true, type.isTokenized());
    assertEquals(false, type.getOmitNorms());
    assertEquals(false, docs.get(0).getField("data1").isStored());
    assertEquals(true, docs.get(0).getField("data2").isTokenized());
    assertEquals(true, docs.get(0).getField("data2").getOmitNorms());
    assertEquals(false, docs.get(0).getField("data3").isStored());
    assertEquals(true, docs.get(0).getField("data3").getOmitNorms());
    assertEquals(false, docs.get(0).getField("data3").isStored());
    assertEquals(false, docs.get(0).getField("data4").isIndexed());
    assertEquals(false, docs.get(0).getField("data5").isStored());
    assertEquals(false, docs.get(0).getField("data5").isTokenized());
    assertEquals(3, docs.get(0).getFields("data6").length);
  }

  // Warning conditions
  // ----------------------------------------------------------------------------------------------

  @Test
  public void testProcess_Warning_BadDateFormat() throws FileNotFoundException, IndexException {
    List<Document> docs = this.parser.process(new File(DATA, "warning-baddateformat.xml"));
    assertEquals(2, docs.size());
  }

  @Test
  public void testProcess_Warning_BadDateValue() throws FileNotFoundException, IndexException {
    List<Document> docs = this.parser.process(new File(DATA, "warning-baddatevalue.xml"));
    assertEquals(2, docs.size());
  }

  @Test
  public void testProcess_Warning_EmptyDocument() throws FileNotFoundException, IndexException {
    List<Document> docs = this.parser.process(new File(DATA, "warning-emptydocument.xml"));
    assertEquals(1, docs.size());
  }

  @Test
  public void testProcess_Warning_NeitherStoreNorIndex() throws FileNotFoundException, IndexException {
    List<Document> docs = this.parser.process(new File(DATA, "warning-neitherstorenorindex.xml"));
    assertEquals(2, docs.size());
  }

  // Error conditions
  // ----------------------------------------------------------------------------------------------

  @Test
  public void testProcess_Error_Invalid() throws FileNotFoundException, IndexException {
    File xml = new File(DATA, "error-invalid.xml");
    InputSource source = new InputSource(new FileInputStream(xml));
    boolean thrown = false;
    try {
      this.parser.process(source);
    } catch (IndexException ex) {
      thrown = true;
    } finally {
      assertTrue("Threw IndexException when invalid", thrown);
    }
  }

  @Test
  public void testProcess_Error_Malformed() throws FileNotFoundException, IndexException {
    File xml = new File(DATA, "error-malformed.xml");
    InputSource source = new InputSource(new FileInputStream(xml));
    boolean thrown = false;
    try {
      this.parser.process(source);
    } catch (IndexException ex) {
      thrown = true;
    } finally {
      assertTrue("Threw IndexException when invalid", thrown);
    }
  }

  /**
   * Asserts that the basic flags for the fields are as specified and that the field is not <code>null</code>. 
   * 
   * @param f         The field to check
   * @param stored    Value of the expected {@link Field#isStored()} return value.
   * @param indexed   Value of the expected {@link Field#isIndexed()} return value.
   * @param tokenized Value of the expected {@link Field#isTokenized()} return value.
   * @param omitNorms Value of the expected {@link Field#getOmitNorms()} return value.
   */
  private static void assertFieldIsSITO(Field f, boolean stored, boolean indexed, boolean tokenized, boolean omitNorms) {
    assertNotNull(f);
    assertEquals(stored,    f.isStored());
    assertEquals(indexed,   f.isIndexed());
    assertEquals(tokenized, f.isTokenized());
    assertEquals(omitNorms, f.getOmitNorms());
  }

}