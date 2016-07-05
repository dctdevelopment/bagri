package com.bagri.xdm.cache.hazelcast.impl;

import static com.bagri.xdm.common.XDMConstants.xdm_config_path;
import static com.bagri.xdm.common.XDMConstants.xdm_config_properties_file;
import static com.bagri.xdm.common.XDMConstants.xdm_document_data_format;
import static com.bagri.xdm.common.XDMConstants.xdm_schema_format_default;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.xquery.XQItem;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bagri.xdm.api.test.XDMManagementTest;
import com.bagri.xdm.client.hazelcast.impl.ResultCursor;
import com.bagri.xdm.domain.Document;
import com.bagri.xdm.system.DataFormat;
import com.bagri.xdm.system.Schema;
import com.bagri.xquery.api.XQProcessor;
import com.bagri.xquery.saxon.extension.StoreDocument;

public class JsonQueryManagementTest extends XDMManagementTest {

    private static ClassPathXmlApplicationContext context;
	
    private XQProcessor xqProc; 

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		sampleRoot = "..\\..\\etc\\samples\\json\\";
		System.setProperty("hz.log.level", "info");
		//System.setProperty("xdm.log.level", "trace");
		System.setProperty("logback.configurationFile", "hz-logging.xml");
		System.setProperty(xdm_config_properties_file, "json.properties");
		System.setProperty(xdm_config_path, "src\\test\\resources");
		context = new ClassPathXmlApplicationContext("spring/cache-test-context.xml");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		context.close();
	}

	@Before
	public void setUp() throws Exception {
		xRepo = context.getBean(SchemaRepositoryImpl.class);
		SchemaRepositoryImpl xdmRepo = (SchemaRepositoryImpl) xRepo; 
		xqProc = context.getBean("xqProcessor", XQProcessor.class);
		Schema schema = xdmRepo.getSchema();
		if (schema == null) {
			schema = new Schema(1, new java.util.Date(), "test", "test", "test schema", true, null);
			schema.setProperty(xdm_schema_format_default, "JSON");
			xdmRepo.setSchema(schema);
			DataFormat df = new DataFormat(1, new java.util.Date(), "", "JSON", null, "application/json", null, 
					"com.bagri.xdm.common.df.json.JsonApiParser", "com.bagri.xdm.common.df.json.JsonBuilder", true, null);
			ArrayList<DataFormat> cFormats = new ArrayList<>(1);
			cFormats.add(df);
			xdmRepo.setDataFormats(cFormats);
			
			long txId = xRepo.getTxManagement().beginTransaction();
			createDocumentTest(sampleRoot + "security1500.json");
			createDocumentTest(sampleRoot + "security5621.json");
			createDocumentTest(sampleRoot + "security9012.json");
			xRepo.getTxManagement().commitTransaction(txId);
		}
	}

	@After
	public void tearDown() throws Exception {
		removeDocumentsTest();
	}
	
	//protected String getFileName(String original) {
	//	return original.substring(0, original.indexOf(".")) + ".json";
	//}
	
	protected Properties getDocumentProperties() {
		Properties props = new Properties();
		props.setProperty(xdm_document_data_format, "JSON");
		return props;
	}

	@Test
	public void convertJsonDocumentsTest() throws Exception {
	
		String query = "for $map in fn:collection()\n" + 
				"let $props := entry('method', 'json')\n" +
				"let $json := fn:serialize($map, $props)\n" +
				"return fn:json-to-xml($json)";
		Iterator<?> docs = getQueryManagement().executeQuery(query, null, new Properties());
		assertNotNull(docs);
		((ResultCursor) docs).deserialize(((SchemaRepositoryImpl) xRepo).getHzInstance());
		Properties props = new Properties();
		//props.setProperty("method", "xml");
		List<String> jsons = new ArrayList<>();
		while (docs.hasNext()) {
			XQItem item = (XQItem) docs.next();
			String json = item.getItemAsString(props);
			jsons.add(json);
			//System.out.println(json);
		}
		assertEquals(3, jsons.size());
	}
	
	@Test
	public void serializeJsonDocumentsTest() throws Exception {
	
		String query = "for $uri in fn:uri-collection()\n" +
				"let $map := fn:json-doc($uri)\n" +
				"let $props := map { 'method': 'json' }\n" +
				"return fn:serialize($map, $props)";
		
		Properties props = new Properties();
		//props.setProperty("method", "json");
		Iterator<?> docs = getQueryManagement().executeQuery(query, null, props);
		assertNotNull(docs);
		((ResultCursor) docs).deserialize(((SchemaRepositoryImpl) xRepo).getHzInstance());
		props = new Properties();
		//props.setProperty("method", "json");
		List<String> jsons = new ArrayList<>();
		while (docs.hasNext()) {
			XQItem item = (XQItem) docs.next();
			String json = item.getItemAsString(props);
			jsons.add(json);
			//System.out.println(json);
		}
		assertEquals(3, jsons.size());
	}

	@Test
	public void getJsonDocumentsTest() throws Exception {
	
		String query = "for $map in fn:collection()\n" + 
				"let $v := get($map, 'Security')\n" +
				//"where get($v, '-id') = '5621'\n" +
				"where get($v, 'Symbol') = 'IBM'\n" +
				"return $v?('Symbol', 'Name')";
		Iterator<?> docs = getQueryManagement().executeQuery(query, null, new Properties());
		assertNotNull(docs);
		((ResultCursor) docs).deserialize(((SchemaRepositoryImpl) xRepo).getHzInstance());
		Properties props = new Properties();
		props.setProperty("method", "text");
		List<String> results = new ArrayList<>();
		while (docs.hasNext()) {
			XQItem item = (XQItem) docs.next();
			String text = item.getItemAsString(props);
			results.add(text);
		}
		assertEquals(2, results.size());
	}
	
}
