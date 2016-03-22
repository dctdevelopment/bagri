package com.bagri.xdm.cache.hazelcast.impl;

import static com.bagri.common.config.XDMConfigConstants.xdm_config_path;
import static com.bagri.common.config.XDMConfigConstants.xdm_config_properties_file;
import static com.bagri.xdm.common.XDMConstants.xs_ns;
import static com.bagri.xdm.common.XDMConstants.xs_prefix;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQItemType;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bagri.common.manage.JMXUtils;
import com.bagri.common.query.AxisType;
import com.bagri.common.query.Comparison;
import com.bagri.common.query.ExpressionContainer;
import com.bagri.common.query.PathBuilder;
import com.bagri.xdm.api.test.XDMManagementTest;
import com.bagri.xdm.cache.api.XDMQueryManagement;
import com.bagri.xdm.domain.XDMOccurence;
import com.bagri.xdm.domain.XDMDocument;
import com.bagri.xdm.domain.XDMNodeKind;
import com.bagri.xdm.system.XDMIndex;
import com.bagri.xdm.system.XDMSchema;

public class UniqueIndexManagementTest extends XDMManagementTest {

    private static ClassPathXmlApplicationContext context;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		sampleRoot = "..\\..\\etc\\samples\\tpox\\";
		System.setProperty("hz.log.level", "info");
		//System.setProperty("xdm.log.level", "trace");
		System.setProperty("logback.configurationFile", "hz-logging.xml");
		System.setProperty(xdm_config_properties_file, "test.properties");
		System.setProperty(xdm_config_path, "src\\test\\resources");
		//context = new ClassPathXmlApplicationContext("spring/cache-xqj-context.xml");
		context = new ClassPathXmlApplicationContext("spring/cache-test-context.xml");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Thread.sleep(3000);
		context.close();
	}

	@Before
	public void setUp() throws Exception {
		xRepo = context.getBean(RepositoryImpl.class);
		RepositoryImpl xdmRepo = (RepositoryImpl) xRepo; 
		XDMSchema schema = xdmRepo.getSchema();
		if (schema == null) {
			schema = new XDMSchema(1, new java.util.Date(), "test", "test", "test schema", true, null);
			xdmRepo.setSchema(schema);
		}
		String typePath = getModelManagement().normalizePath("/{http://tpox-benchmark.com/security}Security");
		XDMIndex index = new XDMIndex(1, new Date(), xRepo.getUserName(), "IDX_Security_Symbol", "/{http://tpox-benchmark.com/security}Security", 
				typePath, "/{http://tpox-benchmark.com/security}Security/{http://tpox-benchmark.com/security}Symbol/text()", new QName(xs_ns, "string", xs_prefix),
				true, true, true, "Security Symbol", true);
		xdmRepo.addSchemaIndex(index);
		
		int docType = xdmRepo.getModelManagement().translateDocumentType("/{http://tpox-benchmark.com/security}Security");
		int pathId = xdmRepo.getModelManagement().translatePath(docType, 
				"/{http://tpox-benchmark.com/security}Security/{http://tpox-benchmark.com/security}Symbol/text()", 
				XDMNodeKind.text, XQItemType.XQBASETYPE_STRING, XDMOccurence.onlyOne).getPathId();
		if (!xdmRepo.getIndexManagement().isPathIndexed(pathId)) {
			System.out.println("path not indexed!!");
		}
	}

	@After
	public void tearDown() throws Exception {
		// remove documents here!
		//getTxManagement().
		removeDocumentsTest();
		//Assert.assertTrue(((IndexManagementImpl) ((RepositoryImpl) xRepo).getIndexManagement()).getIndexCache().size() == 0);
	}
	
	public Collection<String> getSecurity(String symbol) throws Exception {
		String prefix = getModelManagement().getNamespacePrefix("http://tpox-benchmark.com/security"); 
		int docType = 0; //getModelManagement().getDocumentType("/" + prefix + ":Security");
		PathBuilder path = new PathBuilder().
				addPathSegment(AxisType.CHILD, prefix, "Security").
				addPathSegment(AxisType.CHILD, prefix, "Symbol").
				addPathSegment(AxisType.CHILD, null, "text()");
		ExpressionContainer ec = new ExpressionContainer();
		ec.addExpression(docType, Comparison.EQ, path, "$sym", symbol);
		Map<String, String> params = new HashMap<>();
		params.put(":sec", "/" + prefix + ":Security");
		return ((XDMQueryManagement) getQueryManagement()).getContent(ec, ":sec", params);
	}
	
	
	@Test
	public void uniqueDocumentCreateTest() throws Exception {
		long txId = xRepo.getTxManagement().beginTransaction();
		ids.add(createDocumentTest(sampleRoot + getFileName("security5621.xml")).getDocumentKey());
		xRepo.getTxManagement().commitTransaction(txId);

		txId = xRepo.getTxManagement().beginTransaction();
		// this is an update because filename -> uri is the same, 
		// thus no unique index violation expected
		ids.add(createDocumentTest(sampleRoot + getFileName("security5621.xml")).getDocumentKey());
		xRepo.getTxManagement().commitTransaction(txId);

		txId = xRepo.getTxManagement().beginTransaction();
		try {
			ids.add(updateDocumentTest(0, "security1500.xml", sampleRoot + getFileName("security5621.xml")).getDocumentKey());			
			xRepo.getTxManagement().commitTransaction(txId);
			Assert.assertFalse("expected unique index vialation exception", true);
		} catch (Exception ex) {
			// anticipated ex..
			xRepo.getTxManagement().rollbackTransaction(txId);
		}
			
		Collection<String> sec = getSecurity("IBM");
		Assert.assertNotNull(sec);
		Assert.assertTrue("expected 1 but got " + sec.size() + " test documents", sec.size() == 1);
	}

	@Test
	public void uniqueDocumentUpdateTest() throws Exception {
		
		long txId = getTxManagement().beginTransaction();
		XDMDocument doc = createDocumentTest(sampleRoot + getFileName("security1500.xml"));
		Assert.assertNotNull(doc);
		Assert.assertTrue(doc.getTxStart() == txId);
		ids.add(doc.getDocumentKey());
		getTxManagement().commitTransaction(txId);
		long docId = doc.getDocumentId();
		int version = doc.getVersion();
		String uri = doc.getUri();
		
		txId = getTxManagement().beginTransaction();
		doc = updateDocumentTest(0, uri, sampleRoot + getFileName("security1500.xml"));
		Assert.assertNotNull(doc);
		Assert.assertTrue(doc.getTxStart() == txId);
		Assert.assertTrue(doc.getDocumentId() == docId);
		Assert.assertTrue(doc.getVersion() == ++version);
		Assert.assertEquals(doc.getUri(), uri);
		ids.add(doc.getDocumentKey());
		getTxManagement().commitTransaction(txId);
		
		Collection<String> sec = getSecurity("VFINX");
		Assert.assertNotNull(sec);
		Assert.assertTrue("expected 1 but got " + sec.size() + " test documents", sec.size() == 1);
	}
	
	@Test
	public void uniqueDocumentRollbackTest() throws Exception {
		
		long txId = getTxManagement().beginTransaction();
		XDMDocument doc = createDocumentTest(sampleRoot + getFileName("security1500.xml"));
		Assert.assertNotNull(doc);
		Assert.assertTrue(doc.getTxStart() == txId);
		ids.add(doc.getDocumentKey());
		getTxManagement().rollbackTransaction(txId);
		
		txId = getTxManagement().beginTransaction();
		doc = createDocumentTest(sampleRoot + getFileName("security1500.xml"));
		Assert.assertNotNull(doc);
		ids.add(doc.getDocumentKey());
		getTxManagement().commitTransaction(txId);
		
		Collection<String> sec = getSecurity("VFINX");
		Assert.assertNotNull(sec);
		Assert.assertTrue("expected 1 but got " + sec.size() + " test documents", sec.size() == 1);
	}
	
	@Test
	public void uniqueDocumentDeleteTest() throws Exception {
		
		long txId = getTxManagement().beginTransaction();
		XDMDocument doc = createDocumentTest(sampleRoot + getFileName("security1500.xml"));
		Assert.assertNotNull(doc);
		Assert.assertTrue(doc.getTxStart() == txId);
		ids.add(doc.getDocumentKey());
		getTxManagement().commitTransaction(txId);

		txId = getTxManagement().beginTransaction();
		removeDocumentTest(doc.getDocumentKey());
		doc = createDocumentTest(sampleRoot + getFileName("security1500.xml"));
		Assert.assertNotNull(doc);
		ids.add(doc.getDocumentKey());
		getTxManagement().commitTransaction(txId);
		
		Collection<String> sec = getSecurity("VFINX");
		Assert.assertNotNull(sec);
		Assert.assertTrue("expected 1 but got " + sec.size() + " test documents", sec.size() == 1);
	}
	
	@Test
	public void twoDocumentsUpdateTest() throws Exception {

		long txId = getTxManagement().beginTransaction();
		XDMDocument doc = createDocumentTest(sampleRoot + getFileName("security9012.xml"));
		Assert.assertNotNull(doc);
		Assert.assertTrue(doc.getTxStart() == txId);
		ids.add(doc.getDocumentKey());
		getTxManagement().commitTransaction(txId);
		long docId = doc.getDocumentId();
		int version = doc.getVersion();
		String uri = doc.getUri();
		
		txId = getTxManagement().beginTransaction();
		doc = updateDocumentTest(0, uri, sampleRoot + getFileName("security5621.xml"));
		Assert.assertNotNull(doc);
		Assert.assertTrue(doc.getTxStart() == txId);
		Assert.assertTrue(doc.getDocumentId() == docId);
		Assert.assertTrue(doc.getVersion() == ++version);
		Assert.assertEquals(doc.getUri(), uri);
		ids.add(doc.getDocumentKey());
		getTxManagement().commitTransaction(txId);
		
		txId = getTxManagement().beginTransaction();
		doc = createDocumentTest(sampleRoot + getFileName("security9012.xml"));
		Assert.assertNotNull(doc);
		Assert.assertTrue(doc.getTxStart() == txId);
		ids.add(doc.getDocumentKey());
		getTxManagement().commitTransaction(txId);
		//long docId = doc.getDocumentId();
		//int version = doc.getVersion();
		//String uri = doc.getUri();
	}
	
}
