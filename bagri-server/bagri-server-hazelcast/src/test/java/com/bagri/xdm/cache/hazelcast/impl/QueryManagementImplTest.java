package com.bagri.xdm.cache.hazelcast.impl;

import static com.bagri.common.config.XDMConfigConstants.xdm_config_path;
import static com.bagri.common.config.XDMConfigConstants.xdm_config_properties_file;
import static com.bagri.xdm.common.XDMConstants.pn_client_fetchSize;
import static com.bagri.xdm.common.XDMConstants.pn_client_id;
import static com.bagri.xdm.common.XDMConstants.pn_defaultElementTypeNamespace;
import static com.bagri.xdm.common.XDMConstants.pn_baseURI;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;

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
import com.bagri.xdm.cache.hazelcast.impl.RepositoryImpl;
import com.bagri.xdm.client.hazelcast.impl.ResultCursor;
import com.bagri.xdm.system.XDMCollection;
import com.bagri.xdm.system.XDMSchema;
import com.bagri.xquery.api.XQProcessor;

public class QueryManagementImplTest extends XDMManagementTest {

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
		context.close();
	}

	@Before
	public void setUp() throws Exception {
		xRepo = context.getBean(RepositoryImpl.class);
		RepositoryImpl xdmRepo = (RepositoryImpl) xRepo; 
		XDMSchema schema = xdmRepo.getSchema();
		if (schema == null) {
			schema = new XDMSchema(1, new java.util.Date(), "test", "test", "test schema", true, null);
			schema.setProperty(pn_baseURI, sampleRoot);
			xdmRepo.setSchema(schema);
			XDMCollection collection = new XDMCollection(1, new Date(), JMXUtils.getCurrentUser(), 
					1, "CLN_Security", "/{http://tpox-benchmark.com/security}Security", "securities", true);
			schema.addCollection(collection);
		}
	}

	@After
	public void tearDown() throws Exception {
		// remove documents here!
		removeDocumentsTest();
		Thread.sleep(1000);
	}

	
	public Collection<String> getPrice(String symbol) throws Exception {
		String prefix = getModelManagement().getNamespacePrefix("http://tpox-benchmark.com/security"); 
		int docType = 0; //getModelManagement().getDocumentType("/" + prefix + ":Security");
		PathBuilder path = new PathBuilder().
				addPathSegment(AxisType.CHILD, prefix, "Security").
				addPathSegment(AxisType.CHILD, prefix, "Symbol").
				addPathSegment(AxisType.CHILD, null, "text()");
		ExpressionContainer ec = new ExpressionContainer();
		ec.addExpression(docType, Comparison.EQ, path, "$sym", symbol);
		Map<String, String> params = new HashMap<String, String>();
		params.put(":name", "/" + prefix + ":Security/" + prefix + ":Name/text()");
		params.put(":price", "/" + prefix + ":Security/" + prefix + ":Price/" + prefix + ":PriceToday/" + prefix + ":Open/text()");
		return ((XDMQueryManagement) getQueryManagement()).getContent(ec, "<print>The open price of the security \":name\" is :price dollars</print>", params);
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
		Map<String, String> params = new HashMap<String, String>();
		params.put(":sec", "/" + prefix + ":Security");
		return ((XDMQueryManagement) getQueryManagement()).getContent(ec, ":sec", params);
	}
	
	public Collection<String> getOrder(String id) throws Exception {
		String prefix = getModelManagement().getNamespacePrefix("http://www.fixprotocol.org/FIXML-4-4"); 
		int docType = 0; //getModelManagement().getDocumentType("/" + prefix + ":FIXML"); // /" + prefix + ":Order");
		PathBuilder path = new PathBuilder().
				addPathSegment(AxisType.CHILD, prefix, "FIXML").
				addPathSegment(AxisType.CHILD, prefix, "Order").
				addPathSegment(AxisType.ATTRIBUTE, null, "ID");
		ExpressionContainer ec = new ExpressionContainer();
		ec.addExpression(docType, Comparison.EQ, path, "$id", id);
		Map<String, String> params = new HashMap<String, String>();
		params.put(":order", "/" + prefix + ":FIXML/" + prefix + ":Order");
		return ((XDMQueryManagement) getQueryManagement()).getContent(ec, ":order", params);
	}
	
	public Collection<String> getCustomerProfile(String id) throws Exception {
		String prefix = getModelManagement().getNamespacePrefix("http://tpox-benchmark.com/custacc"); 
		int docType = 0; //getModelManagement().getDocumentType("/" + prefix + ":Customer");
		PathBuilder path = new PathBuilder().
				addPathSegment(AxisType.CHILD, prefix, "Customer").
				addPathSegment(AxisType.ATTRIBUTE, null, "id");
		ExpressionContainer ec = new ExpressionContainer();
		ec.addExpression(docType, Comparison.EQ, path, "$id", id);

		String template = "<Customer_Profile CUSTOMERID=\":id\">\n" +
				"\t:name" + 
				"\t:dob" + 
				"\t:gender" + 
				"\t:langs" + 
				"\t:addrs" + 
				"\t:email" + 
			"</Customer_Profile>";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put(":id", "/" + prefix + ":Customer/@id");
		params.put(":name", "/" + prefix + ":Customer/" + prefix + ":Name");
		params.put(":dob", "/" + prefix + ":Customer/" + prefix + ":DateOfBirth");
		params.put(":gender", "/" + prefix + ":Customer/" + prefix + ":Gender");
		params.put(":langs", "/" + prefix + ":Customer/" + prefix + ":Languages");
		params.put(":addrs", "/" + prefix + ":Customer/" + prefix + ":Addresses");
		params.put(":email", "/" + prefix + ":Customer/" + prefix + ":EmailAddresses");
		return ((XDMQueryManagement) getQueryManagement()).getContent(ec, template, params);
	}
	
	public Collection<String> getCustomerAccounts(String id) throws Exception {
		String prefix = getModelManagement().getNamespacePrefix("http://tpox-benchmark.com/custacc"); 
		int docType = 0; //getModelManagement().getDocumentType("/" + prefix + ":Customer");
		PathBuilder path = new PathBuilder().
				addPathSegment(AxisType.CHILD, prefix, "Customer").
				addPathSegment(AxisType.ATTRIBUTE, null, "id");
		ExpressionContainer ec = new ExpressionContainer();
		ec.addExpression(docType, Comparison.EQ, path, "$id", id);

		String template = "<Customer>:id\n" +
				"\t:name" + 
				"\t<Customer_Securities>\n" +
				"\t\t<Account BALANCE=\":balance\" ACCOUNT_ID=\":accId\">\n" +
				"\t\t\t<Securities>\n" +
				"\t\t\t\t:posName" +
				"\t\t\t</Securities>\n" +
				"\t\t</Account>\n" +
				"\t</Customer_Securities>\n" + 
			"</Customer_Profile>";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put(":id", "/" + prefix + ":Customer/@id");
		params.put(":name", "/" + prefix + ":Customer/" + prefix + ":Name");
		params.put(":balance", "/" + prefix + ":Customer/" + prefix + ":Accounts/" + prefix + ":Account/" + prefix + ":Balance/" + prefix + ":OnlineActualBal/text()");
		params.put(":accId", "/" + prefix + ":Customer/" + prefix + ":Accounts/" + prefix + ":Account/@id");
		params.put(":posName", "/" + prefix + ":Customer/" + prefix + ":Accounts/" + prefix + ":Account/" + prefix + ":Holdings/" + prefix + ":Position/" + prefix + ":Name");
		return ((XDMQueryManagement) getQueryManagement()).getContent(ec, template, params);
	}
	
	public Collection<String> searchSecurity(String sector, float peMin, float peMax, float yieldMin) throws Exception {

		String prefix = getModelManagement().getNamespacePrefix("http://tpox-benchmark.com/security"); 
		int docType = 0; //getModelManagement().getDocumentType("/" + prefix + ":Security");
		PathBuilder path = new PathBuilder().
				addPathSegment(AxisType.CHILD, prefix, "Security");
		ExpressionContainer ec = new ExpressionContainer();
		ec.addExpression(docType, Comparison.AND, path);
		ec.addExpression(docType, Comparison.AND, path);
		path.addPathSegment(AxisType.CHILD, prefix, "SecurityInformation").
				addPathSegment(AxisType.CHILD, null, "*").
				addPathSegment(AxisType.CHILD, prefix, "Sector").
				addPathSegment(AxisType.CHILD, null, "text()");
		ec.addExpression(docType, Comparison.EQ, path, "$sec", sector);
		path = new PathBuilder().
				addPathSegment(AxisType.CHILD, prefix, "Security").
				addPathSegment(AxisType.CHILD, prefix, "PE");
		ec.addExpression(docType, Comparison.AND, path);
		path.addPathSegment(AxisType.CHILD, null, "text()");
		ec.addExpression(docType, Comparison.GE, path, "$peMin", new BigDecimal(peMin));
		ec.addExpression(docType, Comparison.LT, path, "$peMax", new BigDecimal(peMax));
		path = new PathBuilder().
				addPathSegment(AxisType.CHILD, prefix, "Security").
				addPathSegment(AxisType.CHILD, prefix, "Yield").
				addPathSegment(AxisType.CHILD, null, "text()");
		ec.addExpression(docType, Comparison.GT, path, "$yMin", new BigDecimal(yieldMin));

        String template = "<Security>\n" +
				"\t:symbol" + 
				"\t:name" + 
				"\t:type" +  
        		//{$sec/SecurityInformation//Sector}
        		//regex = "^/" + prefix + ":Security/" + prefix + ":SecurityInformation/.*/" + prefix + ":Sector$";
        		//Collection<String> sPath = mDictionary.getPathFromRegex(docType, regex);
        		//int idx = 0;
        		//for (String path : sPath) {
        		//	params.put(":sector" + idx, path);
        		//	template += "\t:sector" + idx;
        		//	idx++;
        		//}
				"\t:sector" +  
        		"\t:pe" +
        		"\t:yield" +
		"</Security>";

		Map<String, String> params = new HashMap<String, String>();
		params.put(":symbol", "/" + prefix + ":Security/" + prefix + ":Symbol");
		params.put(":name", "/" + prefix + ":Security/" + prefix + ":Name");
		params.put(":type", "/" + prefix + ":Security/" + prefix + ":SecurityType");
		params.put(":sector", "/" + prefix + ":Security/" + prefix + ":SecurityInformation//" + prefix + ":Sector");
		params.put(":pe", "/" + prefix + ":Security/" + prefix + ":PE");
   		params.put(":yield", "/" + prefix + ":Security/" + prefix + ":Yield");
   		return ((XDMQueryManagement) getQueryManagement()).getContent(ec, template, params);
	}
	
	@Test
	public void getPriceTest() throws Exception {
		storeSecurityTest();

		Collection<String> sec = getPrice("VFINX");
		assertNotNull(sec);
		assertEquals(1, sec.size());

		sec = getPrice("IBM");
		assertNotNull(sec);
		assertEquals(1, sec.size());

		sec = getPrice("PTTAX");
		assertNotNull(sec);
		assertEquals(1, sec.size());
	}

	@Test
	public void getSecurityTest() throws Exception {
		storeSecurityTest();

		Collection<String> sec = getSecurity("VFINX");
		assertNotNull(sec);
		assertEquals(1, sec.size());

		sec = getSecurity("IBM");
		assertNotNull(sec);
		assertEquals(1, sec.size());

		sec = getSecurity("PTTAX");
		assertNotNull(sec);
		assertEquals(1, sec.size());
	}

	@Test
	public void searchSecurityTest() throws Exception {
		storeSecurityTest();

		Collection<String> sec = searchSecurity("Technology", 25, 28, 0);
		assertNotNull(sec);
		assertEquals(1, sec.size());

		sec = searchSecurity("Technology", 25, 28, 1);
		assertNotNull(sec);
		assertEquals(0, sec.size());

		sec = searchSecurity("Technology", 28, 29, 0);
		assertNotNull(sec);
		assertEquals(0, sec.size());
	}

	@Test
	public void getOrderTest() throws Exception {
		storeOrderTest();
		Collection<String> sec = getOrder("103404");
		assertNotNull(sec);
		assertEquals(1, sec.size());
		sec = getOrder("103935");
		assertNotNull(sec);
		assertEquals(1, sec.size());
	}

	@Test
	public void getCustomerProfileTest() throws Exception {
		storeCustomerTest();
		Collection<String> sec = getCustomerProfile("1011");
		assertNotNull(sec);
		assertEquals(1, sec.size());
	}

	@Test
	public void getCustomerAccountsTest() throws Exception {
		storeCustomerTest();
		Collection<String> sec = getCustomerAccounts("1011");
		assertNotNull(sec);
		assertEquals(1, sec.size());
	}
	
	//@Test
	//public void selectDocumentByIdTest() throws Exception {
		
	//	storeSecurityTest();
	//	Map<QName, Object> params = new HashMap<>();
	//	Properties props = new Properties();
	//	props.setProperty(pn_client_id, "1");
	//	props.setProperty(pn_client_fetchSize, "0");
	//	for (Long id: ids) {
	//		String query = "declare namespace s=\"http://tpox-benchmark.com/security\";\n" +
	//			"for $sec in fn:doc(\"bgdm:/" + id + "\")/s:Security\n" +
	//			"return $sec\n";
	//		Iterator itr = xRepo.getQueryManagement().executeQuery(query, params, props);
	//		Assert.assertNotNull(itr);
	//		((ResultCursor) itr).deserialize(((RepositoryImpl) xRepo).getHzInstance());
	//		Assert.assertTrue(itr.hasNext());
	//	}
	//}

	@Test
	public void selectDocumentByUriTest() throws Exception {
		
		storeSecurityTest();
		Map<QName, Object> params = new HashMap<>();
		Properties props = new Properties();
		props.setProperty(pn_client_id, "1");
		props.setProperty(pn_client_fetchSize, "0");
		String[] uris = new String[] {"security1500.xml", "security5621.xml", "security9012.xml"};
		for (String uri: uris) {
			String query = "declare namespace s=\"http://tpox-benchmark.com/security\";\n" +
				"for $sec in fn:doc(\"" + uri + "\")/s:Security\n" +
				"return $sec\n";
			Iterator itr = xRepo.getQueryManagement().executeQuery(query, params, props);
			assertNotNull(itr);
			((ResultCursor) itr).deserialize(((RepositoryImpl) xRepo).getHzInstance());
			assertTrue(itr.hasNext());
		}
	}
	
	@Test
	public void compareSequrityPriceTest() throws Exception {
		storeSecurityTest();
		String query = "declare namespace s=\"http://tpox-benchmark.com/security\";\n" +
			"declare variable $sym external;\n" + 
			//"for $sec in fn:collection(\"CLN_Security\")/s:Security\n" +
			"for $sec in fn:collection()/s:Security\n" +
	  		"where $sec/s:Symbol=$sym\n" + 
			"return \n" +
			"\t<print>The open price of the security \"{$sec/s:Name/text()}\" is {$sec/s:Price/s:PriceToday/s:Open/text()} dollars</print>";
		Map<QName, Object> params = new HashMap<>();
		params.put(new QName("sym"), "VFINX");
		Properties props = new Properties();
		props.setProperty(pn_client_id, "1");
		props.setProperty(pn_client_fetchSize, "1");
		props.setProperty(pn_defaultElementTypeNamespace, "");
		Iterator itr = xRepo.getQueryManagement().executeQuery(query, params, props);
		assertNotNull(itr);
		//((ResultCursor) itr).deserialize(((RepositoryImpl) xRepo).getHzInstance());
		assertTrue(itr.hasNext());
		XQProcessor xqp = ((RepositoryImpl) xRepo).getXQProcessor();
		Object result = itr.next();
		assertNotNull(result);
		String text = xqp.convertToString(result, null);
		assertEquals("<print>The open price of the security \"Vanguard 500 Index Fund\" is 101.12 dollars</print>", text);
		assertFalse(itr.hasNext());
		props = new Properties();
		//props.setProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
		//props.setProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
		props.setProperty(javax.xml.transform.OutputKeys.METHOD, "text");
		text = xqp.convertToString(result, props);
		assertEquals("The open price of the security \"Vanguard 500 Index Fund\" is 101.12 dollars", text);
	}
	
}
