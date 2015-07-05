package com.bagri.xqj;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQExpression;
import javax.xml.xquery.XQItem;
import javax.xml.xquery.XQItemType;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQResultSequence;
import javax.xml.xquery.XQSequence;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.bagri.common.util.FileUtils.readTextFile;

public class BagriXQDataSourceTest {
	
	private XQDataSource xqds;

    @Before
    public void setUp() throws XQException {
    	xqds = new BagriXQDataSource();
	    //xqds.setProperty(BagriXQDataSource.HOST, "127.0.0.1");
	    //xqds.setProperty(BagriXQDataSource.PORT, "5701");
	    xqds.setProperty(BagriXQDataSource.ADDRESS, "localhost:10500");
	    xqds.setProperty(BagriXQDataSource.SCHEMA, "default");
	    xqds.setProperty(BagriXQDataSource.PASSWORD, "password");
	    //xqds.setProperty("hz.cache.mode", "client");
	    xqds.setProperty(BagriXQDataSource.XQ_PROCESSOR, "com.bagri.xquery.saxon.XQProcessorClient");
	    xqds.setProperty(BagriXQDataSource.XDM_REPOSITORY, "com.bagri.xdm.client.hazelcast.impl.RepositoryImpl");
    }
	
	@Test
	public void getConnectionTest() throws XQException {
		XQConnection conn = xqds.getConnection();
		assertNotNull(conn);
		assertFalse(conn.isClosed());
		conn.close();
		assertTrue(conn.isClosed());
	}

	@Test
	@Ignore
	public void getConnectionWithCredentialsTest() throws XQException {
    	String username = "test";
		String password = "test";
		XQConnection conn = xqds.getConnection(username, password);
		assertNull(conn);
		//assertFalse(conn.isClosed());
	}
	
	@Test
	@Ignore
	public void testLoginTimeout() throws XQException {
		xqds.setLoginTimeout(1);
		XQConnection conn = xqds.getConnection();
		assertNull(conn);
		xqds.setLoginTimeout(5);
		conn = xqds.getConnection();
		assertNotNull(conn);
		conn.close();
	}

	@Test
	@Ignore
	public void testQuerySecurity() throws XQException {
		XQConnection xqc = xqds.getConnection();
		assertNotNull(xqc);
		
		String query = "declare namespace s=\"http://tpox-benchmark.com/security\";\n" +
			"declare variable $v external;\n" + // $v\n" +
			//"for $sec in fn:doc(\"sdoc\")/s:Security\n" +
			"for $sec in fn:collection(\"http://tpox-benchmark.com/security\")/s:Security\n" +
	  		"where $sec/s:Symbol=$v\n" + //'IBM'\n" +
			"return\n" +   
			"\t<print>The open price of the security \"{$sec/s:Name/text()}\" is {$sec/s:Price/s:PriceToday/s:Open/text()} dollars</print>\n";

	    XQPreparedExpression xqpe = xqc.prepareExpression(query);
	    xqpe.bindString(new QName("v"), "IBM", null);
	    XQResultSequence xqs = xqpe.executeQuery();
	    assertTrue(xqs.next());
	}

	@Test
	public void testStoreSecurity() throws XQException {

		String dName = "..\\etc\\samples\\tpox\\";
		String xml;
		try {
			xml = readTextFile(dName + "security1500.xml");
		} catch (IOException ex) {
			throw new XQException(ex.getMessage());
		}
		
		XQConnection xqc = xqds.getConnection();
		XQExpression xqe = xqc.createExpression();
		//xqe.bindDocument(new QName("s"), xml, "http://tpox-benchmark.com/security", xqc.createDocumentType());
		xqe.bindString(new QName("s"), xml, xqc.createAtomicType(XQItemType.XQBASETYPE_STRING));
	    xqe.executeCommand("storeDocument($s)");
	    //xqs.next();
	    //assertTrue("expected true but got false", xqs.getBoolean());
	    xqe.close();
	}
	
	@Test
	public void testCreateItemFromDocumentString() throws XQException {

	    XQConnection xqc = xqds.getConnection();
		try {
			xqc.createItemFromDocument((String)null, null, null);
		    fail("A-XQDF-1.2: null argument is invalid and throws an XQException.");
		} catch (XQException e) {
			// Expect an XQException
		}    

		boolean failed = false;
		try {
			XQItem xqitem = xqc.createItemFromDocument("<e>Hello world!</e>", null, xqc.createAtomicType(XQItemType.XQBASETYPE_BOOLEAN));
		    // conversion succeeded, we're having implementation defined behaviour
		    // but at least the XDM instance must be of the right type.
		    if (xqitem.getItemType().getItemKind() != XQItemType.XQITEMKIND_ATOMIC) {
		        failed = true;
		    }
		    if (xqitem.getItemType().getBaseType() != XQItemType.XQBASETYPE_BOOLEAN) {
		        failed = true;
		    }
		} catch (XQException e) {
		    // Expect an XQException
		}   
		if (failed) {
			fail("A-XQDF-1.3: The conversion is subject to the following constraints. " +
					"Either it fails with an XQException, either it is successful in which case it must result in an instance of XDT.");
		}

		try {
			XQItem xqitem = xqc.createItemFromDocument("<e>", null, null);
		    fail("A-XQDF-1.4: The conversion of the value to an XDM instance must fail.");
		} catch (XQException e) {
		    // Expect an XQException
		}    

		XQItem xqi = null;
		try {
		    xqi = xqc.createItemFromDocument("<e>Hello world!</e>", null, null);
		} catch (XQException e) {
		    fail("A-XQDF-1.5: createItemFromDocument() failed with message: " + e.getMessage());
		}
		String result = xqi.getItemAsString(null);
		assertTrue("A-XQDF-1.5: Expects serialized result contains '<e>Hello world!</e>', but it is '" 
					+ result + "'", result.indexOf("<e>Hello world!</e>") != -1);
	}
}
