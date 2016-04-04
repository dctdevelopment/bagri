package com.bagri.xquery.saxon;

import static com.bagri.common.util.FileUtils.readTextFile;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.bagri.common.util.XMLUtils;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.JPConverter;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.ObjectValue;

public class XQProcessorTest {

	private XQProcessorServer parser;

	@Before
	public void setUp() throws Exception {
		parser = new XQProcessorServer();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testConvertToDocument() throws XQException {
		
		String xml = "<e>Hello World!</e>";
		Document doc = parser.convertToDocument(xml);
		assertNotNull(doc);
		assertEquals("Hello World!", doc.getDocumentElement().getTextContent());
	}
	
	@Test
	public void testConvertionSpeed() throws Exception {
		
		//String xml = "<e>Hello World!</e>";
		String fileName = "..\\..\\etc\\samples\\tpox\\security1500.xml";
		String xml = readTextFile(fileName);

		int count = 10000;
		long stamp = System.currentTimeMillis();
		for (int i=0; i < count; i++) {
			Document doc = parser.convertToDocument(xml);
			assertNotNull(doc);
		}
		stamp = System.currentTimeMillis() - stamp;
		System.out.println("Saxon conversion time: " + stamp);

		stamp = System.currentTimeMillis();
		for (int i=0; i < count; i++) {
			Document doc = XMLUtils.textToDocument(xml);
			assertNotNull(doc);
		}
		stamp = System.currentTimeMillis() - stamp;
		System.out.println("Xerces conversion time: " + stamp);
	}
	
	@Test
	public void testQuery() throws XPathException, XQException {

        Configuration config = Configuration.newConfiguration();
        StaticQueryContext sqc = config.newStaticQueryContext();
   	    DynamicQueryContext dqc = new DynamicQueryContext(config);
        dqc.setApplyFunctionConversionRulesToExternalVariables(false);

   	    XQueryExpression xqExp = sqc.compileQuery("declare variable $v external; $v instance of xs:boolean");
		//dqc.setParameter(new StructuredQName("",  "", "v"), BooleanValue.get(true));
		dqc.setParameter(new StructuredQName("",  "", "v"), SaxonUtils.objectToItem(Boolean.TRUE, config));
        SequenceIterator itr = xqExp.iterator(dqc);
        Item item = itr.next();
   	    assertNotNull(item);
   	    String val = item.getStringValue();
		boolean b = Boolean.parseBoolean(val);
		assertTrue(b);

		dqc.clearParameters();
		xqExp = sqc.compileQuery("declare variable $v external; $v instance of xs:byte");
		//dqc.setParameter(new StructuredQName("",  "", "v"), SaxonUtils.objectToItem(new Byte((byte) 1), config));
		dqc.setParameter(new StructuredQName("",  "", "v"), 
				JPConverter.FromByte.INSTANCE.convert(new Byte((byte) 1), config.getConversionContext()));
        itr = xqExp.iterator(dqc);
        item = itr.next();
   	    assertNotNull(item);
   	    val = item.getStringValue();
		b = Boolean.parseBoolean(val);
		//assertTrue(b);
	}

}
